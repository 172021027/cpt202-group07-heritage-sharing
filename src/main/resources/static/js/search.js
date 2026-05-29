$(document).ready(function () {
    let heritageData = [];

    function showContent() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    function escapeHtml(value) {
        return $("<div>").text(value || "").html();
    }

    function isAssetPath(value) {
        return typeof value === "string"
            && value.length > 0
            && (value.startsWith("/") || value.startsWith("uploads/") || value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:"));
    }

    function toAssetUrl(value) {
        if (!isAssetPath(value)) {
            return "";
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:")) {
            return value;
        }
        return value.startsWith("/") ? value : `/${value}`;
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

    function createImageContent(item) {
        const pictureUrl = toAssetUrl(item.picturePath || item.image);
        if (pictureUrl) {
            return `<img src="${pictureUrl}" alt="${escapeHtml(item.title || "Heritage")}" style="width:100%;height:100%;object-fit:cover;">`;
        }
        return `<span class="heritage-image-fallback">${escapeHtml(getFallbackBadge(item))}</span>`;
    }

    function normalizeTags(tags) {
        if (Array.isArray(tags)) {
            return tags
                .map(tag => typeof tag === "string" ? tag.trim() : "")
                .filter(tag => tag);
        }

        if (typeof tags === "string") {
            return tags.split(",").map(tag => tag.trim()).filter(tag => tag);
        }

        return [];
    }

    function normalizeBackendResource(item) {
        const picturePath = item.picturePath || "";
        return {
            id: item.resourceId,
            resourceId: item.resourceId,
            contributorId: item.contributorId ?? null,
            categoryId: item.categoryId ?? null,
            title: item.title || "",
            description: item.description || "",
            category: item.category || "Heritage",
            tags: normalizeTags(item.tags),
            location: item.location || "",
            background: item.background || item.description || "",
            contributors: item.contributors || "",
            date: item.date || "",
            picturePath: picturePath,
            imageUrl: toAssetUrl(picturePath),
            videoPath: item.videoPath || "",
            status: item.status || "",
            typeName: item.category || "Heritage",
            locationName: item.location || ""
        };
    }

    function normalizeFallbackResource(item) {
        const category = item.category || item.typeName || "Heritage";
        const location = item.locationName || item.location || "";
        const picturePath = isAssetPath(item.picturePath) ? item.picturePath : (isAssetPath(item.image) ? item.image : "");
        return {
            id: item.id,
            resourceId: item.resourceId ?? item.id,
            contributorId: item.contributorId ?? null,
            categoryId: item.categoryId ?? null,
            title: item.title || "",
            description: item.description || "",
            category: category,
            tags: normalizeTags(item.tags),
            location: location,
            background: item.background || item.description || "",
            contributors: item.contributors || "",
            date: item.date || "",
            picturePath: picturePath,
            imageUrl: toAssetUrl(picturePath),
            videoPath: isAssetPath(item.videoPath) ? item.videoPath : (item.video && isAssetPath(item.video.url) ? item.video.url : ""),
            status: item.status || "",
            typeName: category,
            locationName: location
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

    function getFallbackResources() {
        const cached = readCachedResources();
        if (Array.isArray(cached) && cached.length > 0) {
            return cached.map(normalizeFallbackResource);
        }

        if (window.AppCommon && typeof window.AppCommon.getHeritageData === "function") {
            return window.AppCommon.getHeritageData().map(normalizeFallbackResource);
        }

        return [];
    }

    function renderHeritageCards(data) {
        const $grid = $("#heritage-grid");
        $grid.empty();

        if (!data.length) {
            $grid.html('<div class="no-results">No approved resources found.</div>');
            return;
        }

        data.forEach(item => {
            const $card = $('<div class="heritage-card" style="cursor: pointer;"></div>').attr("data-id", item.id);
            const $image = $('<div class="heritage-image"></div>');

            if (item.imageUrl) {
                $("<img>")
                    .attr("src", item.imageUrl)
                    .attr("alt", item.title)
                    .attr("style", "width:100%;height:100%;object-fit:cover;")
                    .appendTo($image);
            } else {
                $image.text("No image");
            }

            const $content = $('<div class="heritage-content"></div>');
            $("<h4></h4>").text(item.title).appendTo($content);
            $("<p></p>").text(item.description).appendTo($content);

            const $meta = $('<div class="heritage-meta"></div>');
            $('<span class="heritage-type"></span>').text(item.typeName).appendTo($meta);
            $("<span></span>").text(item.locationName).appendTo($meta);

            $content.append($meta);
            $card.append($image, $content);
            $grid.append($card);
        });
    }

    function loadHeritageData() {
        $.ajax({
            url: "/api/resources/approved",
            method: "GET",
            success: function (response) {
                heritageData = Array.isArray(response) ? response.map(normalizeBackendResource) : [];
                persistResourceCache(heritageData);
                renderHeritageCards(heritageData);
                showContent();
            },
            error: function () {
                heritageData = getFallbackResources();
                persistResourceCache(heritageData);
                renderHeritageCards(heritageData);
                showContent();
            }
        });
    }

    function initPage() {
        if (window.AuthState && typeof window.AuthState.requireAuthenticated === "function") {
            window.AuthState.requireAuthenticated({
                onPass: function () {
                    loadHeritageData();
                }
            });
            return;
        }

        const token = localStorage.getItem("token");
        if (!token) {
            if (window.AuthState && typeof window.AuthState.redirectToLogin === "function") {
                window.AuthState.redirectToLogin();
            } else {
                window.location.href = "login.html";
            }
            return;
        }

        loadHeritageData();
    }

    initPage();

    $(document).on("click", ".heritage-card", function () {
        const id = $(this).data("id");
        window.location.href = "detail.html?id=" + encodeURIComponent(id);
    });

    $("#search-btn").click(function () {
        const searchTerm = $("#search-input").val().trim();
        if (searchTerm) {
            sessionStorage.setItem("searchTerm", searchTerm);
            window.location.href = "detail-search.html";
        } else {
            alert("Please enter a search term");
        }
    });

    $("#search-input").keypress(function (e) {
        if (e.which === 13) {
            $("#search-btn").click();
        }
    });
});
