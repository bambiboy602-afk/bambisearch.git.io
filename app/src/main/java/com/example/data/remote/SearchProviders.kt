package com.example.data.remote

import com.example.data.model.FileTypeBadge
import com.example.data.model.RawSearchResult
import com.example.data.model.SearchFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

interface SearchProvider {
    val name: String
    val isSpecialist: Boolean
    suspend fun search(query: String, maxResults: Int = 10, filters: SearchFilters = SearchFilters()): List<RawSearchResult>
}

object SearchUtils {
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun detectFileType(url: String, text: String = ""): FileTypeBadge {
        val lower = (url + " " + text).lowercase(Locale.ROOT)
        return when {
            lower.contains(".pdf") || lower.contains("format: pdf") -> FileTypeBadge.PDF
            lower.contains(".doc") || lower.contains(".docx") || lower.contains(".odt") -> FileTypeBadge.DOC
            lower.contains(".csv") || lower.contains(".tsv") || lower.contains(".json") || lower.contains(".sqlite") || lower.contains("dataset") -> FileTypeBadge.DATASET
            lower.contains(".zip") || lower.contains(".tar") || lower.contains(".gz") || lower.contains(".7z") || lower.contains("archive") -> FileTypeBadge.ARCHIVE
            lower.contains(".py") || lower.contains(".c") || lower.contains(".cpp") || lower.contains(".sh") || lower.contains(".asm") || lower.contains(".go") || lower.contains(".rs") -> FileTypeBadge.CODE
            lower.contains(".txt") || lower.contains(".md") || lower.contains("readme") -> FileTypeBadge.TXT
            else -> FileTypeBadge.WEB
        }
    }

    fun cleanHtml(raw: String): String {
        return raw.replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

class InternetArchiveProvider : SearchProvider {
    override val name: String = "Internet Archive"
    override val isSpecialist: Boolean = true

    override suspend fun search(query: String, maxResults: Int, filters: SearchFilters): List<RawSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawSearchResult>()
        try {
            var effectiveQuery = query
            val dateClause = filters.buildArchiveDateClause()
            val formatClause = filters.buildArchiveFormatClause()
            if (dateClause != null) effectiveQuery = "($effectiveQuery) AND $dateClause"
            if (formatClause != null) effectiveQuery = "($effectiveQuery) AND $formatClause"

            val encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8.toString())
            // Query Archive.org advanced search for texts, movies, software, data
            val url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,description,date,mediatype,format&sort[]=&rows=$maxResults&page=1&output=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LeadTrace-Research-Bot/1.0")
                .build()

            SearchUtils.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val responseObj = json.optJSONObject("response") ?: return@withContext emptyList()
                val docs = responseObj.optJSONArray("docs") ?: JSONArray()

                for (i in 0 until docs.length()) {
                    val doc = docs.getJSONObject(i)
                    val id = doc.optString("identifier")
                    val title = doc.optString("title", id)
                    val desc = doc.optString("description", "Archived item identifier: $id")
                    val date = doc.optString("date", "Undated Archive")
                    val directLink = "https://archive.org/details/$id"
                    val fileType = SearchUtils.detectFileType(directLink, doc.toString())

                    results.add(
                        RawSearchResult(
                            title = SearchUtils.cleanHtml(title),
                            url = directLink,
                            snippet = SearchUtils.cleanHtml(desc.take(300)),
                            provider = name,
                            date = date,
                            detectedType = fileType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Graceful fallback to preserve stability
        }
        results
    }
}

class GitHubSearchProvider : SearchProvider {
    override val name: String = "GitHub Repositories"
    override val isSpecialist: Boolean = true

    override suspend fun search(query: String, maxResults: Int, filters: SearchFilters): List<RawSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawSearchResult>()
        try {
            var effectiveQuery = query
            if (filters.dateRange.startYear != null) {
                effectiveQuery += " created:>=${filters.dateRange.startYear}-01-01"
            }
            if (filters.fileType.extensions.isNotEmpty()) {
                effectiveQuery += " extension:${filters.fileType.extensions.first()}"
            }
            val encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8.toString())
            val url = "https://api.github.com/search/repositories?q=$encodedQuery&sort=stars&order=desc&per_page=$maxResults"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "LeadTrace-Research-Agent-Android")
                .build()

            SearchUtils.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: JSONArray()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val fullName = item.optString("full_name")
                    val htmlUrl = item.optString("html_url")
                    val description = item.optString("description", "Repository: $fullName")
                    val stars = item.optInt("stargazers_count", 0)
                    val language = item.optString("language", "Code")
                    val updatedAt = item.optString("updated_at", "")

                    val dateStr = if (updatedAt.length >= 10) updatedAt.substring(0, 10) else "Recent"
                    val snippet = "[$language | ⭐ $stars] $description"

                    results.add(
                        RawSearchResult(
                            title = fullName,
                            url = htmlUrl,
                            snippet = snippet,
                            provider = name,
                            date = dateStr,
                            detectedType = FileTypeBadge.CODE
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Continue gracefully
        }
        results
    }
}

class RedditSearchProvider : SearchProvider {
    override val name: String = "Reddit Communities"
    override val isSpecialist: Boolean = true

    override suspend fun search(query: String, maxResults: Int, filters: SearchFilters): List<RawSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawSearchResult>()
        try {
            val effectiveQuery = filters.applyToGeneralQuery(query)
            val encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8.toString())
            val url = "https://www.reddit.com/search.json?q=$encodedQuery&limit=$maxResults&sort=relevance"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "android:com.example.leadtrace:v1.0 (by /u/LeadTraceBot)")
                .build()

            SearchUtils.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val children = data.optJSONArray("children") ?: JSONArray()

                for (i in 0 until children.length()) {
                    val child = children.getJSONObject(i)
                    val childData = child.optJSONObject("data") ?: continue
                    val title = childData.optString("title")
                    val permalink = childData.optString("permalink")
                    val externalUrl = childData.optString("url", "")
                    val subreddit = childData.optString("subreddit_name_prefixed")
                    val selftext = childData.optString("selftext", "")
                    val createdUtc = childData.optLong("created_utc", 0)

                    val directUrl = if (externalUrl.isNotBlank() && !externalUrl.contains("reddit.com")) {
                        externalUrl
                    } else {
                        "https://www.reddit.com$permalink"
                    }

                    val dateStr = if (createdUtc > 0) {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(createdUtc * 1000))
                    } else "Archive"

                    val snippet = "[$subreddit] " + if (selftext.isNotBlank()) selftext.take(280) else title

                    results.add(
                        RawSearchResult(
                            title = SearchUtils.cleanHtml(title),
                            url = directUrl,
                            snippet = SearchUtils.cleanHtml(snippet),
                            provider = name,
                            date = dateStr,
                            detectedType = SearchUtils.detectFileType(directUrl, snippet)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Continue gracefully
        }
        results
    }
}

class WikipediaProvider : SearchProvider {
    override val name: String = "Wikimedia / Open Records"
    override val isSpecialist: Boolean = false

    override suspend fun search(query: String, maxResults: Int, filters: SearchFilters): List<RawSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawSearchResult>()
        try {
            val effectiveQuery = filters.applyToGeneralQuery(query)
            val encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8.toString())
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&utf8=1&srlimit=$maxResults"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LeadTrace-Research-Bot/1.0 (https://leadtrace.app)")
                .build()

            SearchUtils.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val queryObj = json.optJSONObject("query") ?: return@withContext emptyList()
                val searchList = queryObj.optJSONArray("search") ?: JSONArray()

                for (i in 0 until searchList.length()) {
                    val item = searchList.getJSONObject(i)
                    val title = item.optString("title")
                    val snippet = SearchUtils.cleanHtml(item.optString("snippet"))
                    val timestamp = item.optString("timestamp", "")
                    val pageUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8.toString())
                    val dateStr = if (timestamp.length >= 10) timestamp.substring(0, 10) else "Verified Record"

                    results.add(
                        RawSearchResult(
                            title = title,
                            url = pageUrl,
                            snippet = snippet,
                            provider = name,
                            date = dateStr,
                            detectedType = FileTypeBadge.WEB
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Continue gracefully
        }
        results
    }
}

class FirecrawlWebProvider(
    private var customApiKey: String = ""
) : SearchProvider {
    override val name: String = "Firecrawl & Web Extraction"
    override val isSpecialist: Boolean = false

    fun updateApiKey(key: String) {
        customApiKey = key
    }

    override suspend fun search(query: String, maxResults: Int, filters: SearchFilters): List<RawSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawSearchResult>()
        val effectiveQuery = filters.applyToGeneralQuery(query)
        // 1. If customApiKey is set, attempt Firecrawl API search endpoint
        if (customApiKey.isNotBlank() && customApiKey != "MY_FIRECRAWL_API_KEY") {
            try {
                val jsonPayload = JSONObject().apply {
                    put("query", effectiveQuery)
                    put("limit", maxResults)
                }
                val request = Request.Builder()
                    .url("https://api.firecrawl.dev/v1/search")
                    .header("Authorization", "Bearer $customApiKey")
                    .header("Content-Type", "application/json")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                SearchUtils.httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val data = json.optJSONArray("data") ?: JSONArray()
                            for (i in 0 until data.length()) {
                                val item = data.getJSONObject(i)
                                val title = item.optString("title", "Web Discovery")
                                val url = item.optString("url")
                                val markdown = item.optString("markdown", "")
                                val description = item.optString("description", markdown.take(200))
                                results.add(
                                    RawSearchResult(
                                        title = title,
                                        url = url,
                                        snippet = SearchUtils.cleanHtml(description),
                                        provider = name,
                                        date = "Live Web",
                                        detectedType = SearchUtils.detectFileType(url, description)
                                    )
                                )
                            }
                            if (results.isNotEmpty()) return@withContext results
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall back to open web search
            }
        }

        // 2. Open Archive & Open Search web fallback
        try {
            val encodedQuery = URLEncoder.encode(effectiveQuery, StandardCharsets.UTF_8.toString())
            // Query DuckDuckGo instant answer / abstract API
            val ddgUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "LeadTrace-Research-Bot/1.0")
                .build()

            SearchUtils.httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val abstractText = json.optString("AbstractText", "")
                        val abstractUrl = json.optString("AbstractURL", "")
                        val heading = json.optString("Heading", query)
                        if (abstractUrl.isNotBlank() && abstractText.isNotBlank()) {
                            results.add(
                                RawSearchResult(
                                    title = heading,
                                    url = abstractUrl,
                                    snippet = abstractText,
                                    provider = "Web Knowledge Graph",
                                    date = "Current",
                                    detectedType = SearchUtils.detectFileType(abstractUrl, abstractText)
                                )
                            )
                        }
                        val relatedTopics = json.optJSONArray("RelatedTopics") ?: JSONArray()
                        for (i in 0 until Math.min(relatedTopics.length(), maxResults)) {
                            val topic = relatedTopics.optJSONObject(i) ?: continue
                            val topicText = topic.optString("Text", "")
                            val firstUrl = topic.optString("FirstURL", "")
                            if (firstUrl.isNotBlank() && topicText.isNotBlank()) {
                                results.add(
                                    RawSearchResult(
                                        title = topicText.take(60),
                                        url = firstUrl,
                                        snippet = topicText,
                                        provider = "Web Knowledge Graph",
                                        date = "Reference",
                                        detectedType = SearchUtils.detectFileType(firstUrl, topicText)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        results
    }
}
