$(document).ready(function () {
    function getIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return params.get("id");
    }

    function getAuthHeaders() {
        const token = localStorage.getItem("token");
        return token ? { Authorization: "Bearer " + token } : {};
    }

    function valueOrFallback(value, fallback = "N/A") {
        if (value === null || value === undefined) {
            return fallback;
        }

        const normalized = String(value).trim();
        return normalized === "" ? fallback : normalized;
    }

    function formatDateTime(value) {
        if (!value) {
            return "";
        }
        const normalized = value.includes("T") ? value : value.replace(" ", "T");
        const date = new Date(normalized);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    }

    function loadComments() {
        $.ajax({
            url: "/api/resource-comments/resource/" + encodeURIComponent(id),
            method: "GET",
            headers: getAuthHeaders(),
            success: function (comments) {
                $("#comment-list").empty();
                if (!comments || comments.length === 0) {
                    $("#comment-list").html('<p class="empty-comments">No comments yet.</p>');
                    return;
                }
                comments.forEach(renderComment);
            },
            error: function () {
                $("#comment-list").html('<p class="empty-comments">Unable to load comments.</p>');
            }
        });
    }

    function renderComment(comment, prepend = false) {
        const template = document.getElementById("comment-template");
        const commentNode = template.content.cloneNode(true);
        const $comment = $(commentNode).find(".comment-item");

        $comment.attr("data-comment-id", comment.commentId);
        $comment.find(".comment-user strong").text(comment.username + "#" + String(comment.userId).padStart(8, "0") || "Unknown User");
        $comment.find(".comment-time").text(formatDateTime(comment.commentedAt));
        $comment.find(".comment-text").text(comment.commentText || "");

        if (!comment.canDelete) {
            $comment.find(".delete-btn").remove();
        }

        $(".empty-comments").remove();
        if (prepend) {
            $("#comment-list").prepend(commentNode);
        } else {
            $("#comment-list").append(commentNode);
        }
    }
    function escapeHtml(value) {
        return $("<div>").text(value || "").html();
    }

    // Resource paths can come from Windows-style uploads, browser-safe URLs,
    // or older cached data. Normalize once so image/video rendering only deals
    // with one URL shape.
    function normalizeAssetPath(value) {
        return typeof value === "string" ? value.replace(/\\/g, "/") : "";
    }

    function isAssetPath(value) {
        const normalized = normalizeAssetPath(value);
        return normalized.length > 0
            && (normalized.startsWith("/")
                || normalized.startsWith("uploads/")
                || normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("data:"));
    }

    function toAssetUrl(value) {
        const normalized = normalizeAssetPath(value);
        if (!isAssetPath(normalized)) {
            return "";
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("data:")) {
            return normalized;
        }
        return normalized.startsWith("/") ? normalized : `/${normalized}`;
    }

    function getFallbackBadge(item) {
        const category = `${item.category || item.typeName || item.type || ""}`.toLowerCase();
        if (category.includes("artifact")) {
            return "ART";
        }
        if (category.includes("tradition") || category.includes("festival") || category.includes("culture")) {
            return "CUL";
        }
        if (category.includes("natural")) {
            return "NAT";
        }
        if (category.includes("city")) {
            return "CTY";
        }
        return "HRT";
    }

    function buildPlaceholderImage(item) {
        const label = getFallbackBadge(item);
        const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="700" viewBox="0 0 1200 700"><rect width="1200" height="700" fill="#ece4d5"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#6f6658" font-family="Arial, sans-serif" font-size="84">${label}</text></svg>`;
        return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
    }

    // The public API now exposes tags as a collection, but cached fallback data
    // may still hold comma-joined strings or tag-like objects from older UI code.
    function normalizeTags(tags) {
        if (Array.isArray(tags)) {
            return tags
                .map(tag => {
                    if (typeof tag === "string") {
                        return tag.trim();
                    }
                    if (tag && typeof tag === "object") {
                        return (tag.tagName || tag.name || tag.label || "").trim();
                    }
                    return "";
                })
                .filter(Boolean);
        }

        if (typeof tags === "string") {
            return tags
                .split(",")
                .map(tag => tag.trim())
                .filter(Boolean);
        }

        return [];
    }

    function formatTags(tags) {
        const tagNames = normalizeTags(tags);
        return tagNames.length > 0 ? tagNames.join(", ") : "No tags";
    }

    // Keep the page renderer bound to one normalized view-model even when the
    // source is the live public API.
    function normalizeBackendResource(item) {
        const category = item.category || "Heritage";
        return {
            id: item.resourceId ?? item.id,
            resourceId: item.resourceId ?? item.id,
            contributorId: item.contributorId ?? null,
            categoryId: item.categoryId ?? null,
            title: item.title || "",
            description: item.description || "",
            category: category,
            tags: normalizeTags(item.tags),
            location: item.location || "",
            contributors: item.contributors ?? item.contributor ?? "",
            date: item.date || "",
            picturePath: item.picturePath ?? item.imageUrl ?? "",
            videoPath: item.videoPath ?? (item.video && item.video.url ? item.video.url : ""),
            status: item.status || ""
        };
    }

    // Search results, AppCommon seed data, and localStorage may still use older
    // field names. Normalize them into the same shape used by server responses.
    function normalizeFallbackResource(item) {
        const category = item.category || item.typeName || "Heritage";
        const location = item.locationName || item.location || "";
        const picturePath = item.picturePath || item.image || item.imageUrl || "";
        const videoPath = item.videoPath || (item.video && item.video.url ? item.video.url : "");

        return {
            id: item.id ?? item.resourceId,
            resourceId: item.resourceId ?? item.id,
            contributorId: item.contributorId || null,
            categoryId: item.categoryId || null,
            title: item.title || "",
            description: item.description || "",
            category: category,
            tags: normalizeTags(item.tags),
            location: location,
            contributors: item.contributors ?? item.contributor ?? "",
            date: item.date || "",
            picturePath: isAssetPath(picturePath) ? picturePath : "",
            videoPath: isAssetPath(videoPath) ? videoPath : "",
            status: item.status || ""
        };
    }

    function persistResourceCache(data) {
        if (window.AppCommon && typeof window.AppCommon.persistHeritageData === "function") {
            window.AppCommon.persistHeritageData(data);
            return;
        }
        localStorage.setItem("heritageData", JSON.stringify(data));
    }

    function readCachedResources() {
        if (window.AppCommon && typeof window.AppCommon.readHeritageDataFromStorage === "function") {
            return window.AppCommon.readHeritageDataFromStorage();
        }

        const raw = localStorage.getItem("heritageData");
        if (!raw) {
            return [];
        }

        try {
            return JSON.parse(raw);
        } catch (error) {
            return [];
        }
    }

    // Cache the latest successful detail payload so the detail page can still
    // render if the user navigates back from search data or a later request fails.
    function mergeResourceIntoCache(resource) {
        const cached = readCachedResources().map(normalizeFallbackResource);
        const nextCache = [resource].concat(
            cached.filter(item => String(item.id) !== String(resource.id))
        );
        persistResourceCache(nextCache);
    }

    function getFallbackResource(id) {
        const cached = readCachedResources();
        const normalizedCached = Array.isArray(cached) ? cached.map(normalizeFallbackResource) : [];
        const cachedMatch = normalizedCached.find(item => String(item.id) === String(id));
        if (cachedMatch) {
            return cachedMatch;
        }

        if (window.AppCommon && typeof window.AppCommon.getHeritageData === "function") {
            return window.AppCommon.getHeritageData()
                .map(normalizeFallbackResource)
                .find(item => String(item.id) === String(id)) || null;
        }

        return null;
    }

    // detail.html hides media containers by default, so rendering must both bind
    // URLs and explicitly toggle visibility after normalization.
    function renderResource(item) {
        $("#title").text(valueOrFallback(item.title, "Untitled Resource"));
        $("#description").text(valueOrFallback(item.description));
        $("#category").text(valueOrFallback(item.category));
        $("#tags").text(formatTags(item.tags));
        $("#location").text(valueOrFallback(item.location));
        $("#contributors").text(valueOrFallback(item.contributors + "#" + String(item.contributorId).padStart(8, "0")));
        $("#date").text(valueOrFallback(item.date));

        const $image = $("#image");
        const imageUrl = toAssetUrl(item.picturePath) || buildPlaceholderImage(item);
        $image
            .attr("src", imageUrl)
            .attr("alt", item.title || "Heritage")
            .show();

        const $videoContainer = $("#video-container");
        const $videoPlayer = $("#video-player");
        const $videoTitle = $("#video-display-title");
        const videoUrl = toAssetUrl(item.videoPath);

        if (videoUrl) {
            $videoPlayer.attr("src", videoUrl);
            $videoTitle.text(item.title || "Related Media Content");
            $videoContainer.show();
        } else {
            $videoPlayer.removeAttr("src");
            $videoTitle.text("");
            $videoContainer.hide();
        }
    }

    function showMissingResource(message) {
        $(".detail-container").first().html(`<p>${escapeHtml(message)}</p>`);
        $(".detail-container").slice(1).hide();
    }

    const id = getIdFromUrl();
    if (!id) {
        showMissingResource("Invalid request: Missing ID");
        return;
    }

    function showContent() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    // Prefer the approved-only public endpoint. Cached data is only a resilience
    // fallback when the request fails or the page was opened from older UI state.
    $.ajax({
        url: `/api/resources/${id}`,
        method: "GET",
        success: function (response) {
            const resource = normalizeBackendResource(response);
            mergeResourceIntoCache(resource);
            renderResource(resource);
            showContent();
        },
        error: function () {
            const fallbackResource = getFallbackResource(id);
            if (fallbackResource) {
                renderResource(fallbackResource);
                showContent();
                return;
            }
            showMissingResource("Heritage item not found");
            showContent();
        }
    });

    $("#submit-comment").click(function () {
        const content = $("#comment-input").val().trim();
        const $error = $("#comment-error");
        const token = localStorage.getItem("token");

        if (!token) {
            $error.text("Please log in before posting a comment.").show();
            return;
        }

        if (!content) {
            $error.text("Content cannot be empty").show();
            return;
        }

        $error.hide();
        $("#submit-comment").prop("disabled", true);

        $.ajax({
            url: "/api/resource-comments/add",
            method: "POST",
            headers: getAuthHeaders(),
            contentType: "application/json",
            data: JSON.stringify({
                resourceId: Number(id),
                commentText: content
            }),
            success: function (comment) {
                renderComment(comment, true);
                $("#comment-input").val("");
            },
            error: function (xhr) {
                const message = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : "Failed to post comment.";
                $error.text(message).show();
            },
            complete: function () {
                $("#submit-comment").prop("disabled", false);
            }
        });
    });

    $(document).on("click", ".delete-btn", function () {
        const $comment = $(this).closest(".comment-item");
        const commentId = $comment.data("comment-id");

        if (!commentId || !confirm("Delete this comment?")) {
            return;
        }

        $.ajax({
            url: "/api/resource-comments/" + encodeURIComponent(commentId),
            method: "DELETE",
            headers: getAuthHeaders(),
            success: function () {
                $comment.remove();
                if ($("#comment-list .comment-item").length === 0) {
                    $("#comment-list").html('<p class="empty-comments">No comments yet.</p>');
                }
            },
            error: function (xhr) {
                const message = xhr.responseJSON && xhr.responseJSON.message
                    ? xhr.responseJSON.message
                    : "Failed to delete comment.";
                alert(message);
            }
        });
    });

    loadComments();
});
