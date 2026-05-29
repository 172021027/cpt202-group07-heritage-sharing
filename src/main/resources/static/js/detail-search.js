$(document).ready(function () {
    let heritageData = [];
    let searchResultData = [];
    let activeSearchKeyword = "";

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
        const category = item.category || "Heritage";
        return {
            id: item.resourceId,
            resourceId: item.resourceId,
            contributorId: item.contributorId,
            categoryId: item.categoryId,
            categoryKey: item.categoryId != null ? String(item.categoryId) : category.toLowerCase(),
            title: item.title || "",
            description: item.description || "",
            category: category,
            tags: normalizeTags(item.tags),
            location: item.location || "",
            background: item.background || item.description || "",
            contributors: item.contributors || "",
            date: item.date || "",
            picturePath: item.picturePath || "",
            videoPath: item.videoPath || "",
            status: item.status || ""
        };
    }

    function normalizeFallbackResource(item) {
        const category = item.category || item.typeName || "Heritage";
        const location = item.locationName || item.location || "";
        return {
            id: item.id ?? item.resourceId,
            resourceId: item.resourceId ?? item.id,
            contributorId: item.contributorId || null,
            categoryId: item.categoryId || null,
            categoryKey: item.categoryId != null ? String(item.categoryId) : category.toLowerCase(),
            title: item.title || "",
            description: item.description || "",
            category: category,
            tags: normalizeTags(item.tags),
            location: location,
            background: item.background || item.description || "",
            contributors: item.contributors || "",
            date: item.date || "",
            picturePath: isAssetPath(item.picturePath) ? item.picturePath : (isAssetPath(item.image) ? item.image : ""),
            videoPath: isAssetPath(item.videoPath) ? item.videoPath : (item.video && isAssetPath(item.video.url) ? item.video.url : ""),
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

    function renderResults(data) {
        const grid = $("#results-grid");
        grid.empty();

        if (!data.length) {
            grid.html('<div class="no-results">No results found</div>');
            return;
        }

        data.forEach(item => {
            const card = `
                <div class="heritage-card" data-id="${item.id}">
                    <div class="heritage-image">${createImageContent(item)}</div>
                    <div class="heritage-content">
                        <h4>${escapeHtml(item.title)}</h4>
                        <p>${escapeHtml(item.description)}</p>
                        <div class="heritage-meta">
                            <span class="heritage-type">${escapeHtml(item.category)}</span>
                            <span>${escapeHtml(item.location)}</span>
                        </div>
                    </div>
                </div>
            `;
            grid.append(card);
        });
    }

    function populateFilterOptions(data) {
        const currentCategory = $("#filter-category").val();
        const currentTag = $("#filter-tag").val();
        const currentLocation = $("#filter-location").val();
        const categoryOptions = new Map();
        const tagOptions = new Map();
        const locationOptions = new Map();

        data.forEach(item => {
            if (item.category && item.categoryKey) {
                categoryOptions.set(item.categoryKey, item.category);
            }
            item.tags.forEach(tag => {
                tagOptions.set(tag, tag);
            });
            if (item.location) {
                locationOptions.set(item.location, item.location);
            }
        });

        const $categorySelect = $("#filter-category");
        $categorySelect.empty().append('<option value="">All</option>');
        categoryOptions.forEach((label, value) => {
            $categorySelect.append($("<option>").val(value).text(label));
        });
        if (currentCategory && categoryOptions.has(currentCategory)) {
            $categorySelect.val(currentCategory);
        }

        const $tagSelect = $("#filter-tag");
        $tagSelect.empty().append('<option value="">All</option>');
        tagOptions.forEach((label, value) => {
            $tagSelect.append($("<option>").val(value).text(label));
        });
        if (currentTag && tagOptions.has(currentTag)) {
            $tagSelect.val(currentTag);
        }

        const $locationSelect = $("#filter-location");
        $locationSelect.empty().append('<option value="">All</option>');
        locationOptions.forEach((label, value) => {
            $locationSelect.append($("<option>").val(value).text(label));
        });
        if (currentLocation && locationOptions.has(currentLocation)) {
            $locationSelect.val(currentLocation);
        }
    }

    function matchesKeyword(item, keyword) {
        if (!keyword) {
            return true;
        }

        const normalizedKeyword = keyword.toLowerCase();
        const title = (item.title || "").toLowerCase();
        const description = (item.description || "").toLowerCase();
        return title.includes(normalizedKeyword) || description.includes(normalizedKeyword);
    }

    function getSelectedFilters() {
        return {
            keyword: $("#search-input").val().trim(),
            categoryValue: $("#filter-category").val(),
            tag: $("#filter-tag").val(),
            location: $("#filter-location").val()
        };
    }

    function filterSearchResults(filters) {
        return searchResultData.filter(item => {
            if (filters.categoryValue && item.categoryKey !== filters.categoryValue) {
                return false;
            }

            if (filters.tag) {
                const selectedTag = filters.tag.toLowerCase();
                const hasMatchingTag = item.tags.some(tag => tag.toLowerCase() === selectedTag);
                if (!hasMatchingTag) {
                    return false;
                }
            }

            if (filters.location && item.location !== filters.location) {
                return false;
            }

            return true;
        });
    }

    function getSearchFallbackResults(keyword) {
        return heritageData.filter(item => matchesKeyword(item, keyword));
    }

    function renderCurrentResults() {
        populateFilterOptions(searchResultData);
        renderResults(filterSearchResults(getSelectedFilters()));
    }

    function updateSearchResults(keyword, onComplete) {
        const params = {};

        if (keyword) {
            params.keyword = keyword;
        }

        const finalize = function (data) {
            searchResultData = Array.isArray(data) ? data : [];
            activeSearchKeyword = keyword;
            renderCurrentResults();
            if (typeof onComplete === "function") {
                onComplete();
            }
        };

        if (!Object.keys(params).length) {
            finalize(heritageData.slice());
            return;
        }

        $.ajax({
            url: "/api/resources/search",
            method: "GET",
            data: params,
            success: function (response) {
                finalize(Array.isArray(response) ? response.map(normalizeBackendResource) : []);
            },
            error: function () {
                finalize(getSearchFallbackResults(keyword));
            }
        });
    }

    function applyFilters(options = {}) {
        const filters = getSelectedFilters();
        const keywordChanged = filters.keyword !== activeSearchKeyword;
        if (options.forceSearchRefresh || keywordChanged) {
            updateSearchResults(filters.keyword);
            return;
        }

        renderCurrentResults();
    }

    function restoreSearchTerm() {
        const storedSearchTerm = sessionStorage.getItem("searchTerm");
        if (!storedSearchTerm) {
            return false;
        }

        $("#search-input").val(storedSearchTerm);
        sessionStorage.removeItem("searchTerm");
        return true;
    }

    function handleInitialRender() {
        if (restoreSearchTerm()) {
            applyFilters({ forceSearchRefresh: true });
            return;
        }
        searchResultData = heritageData.slice();
        activeSearchKeyword = "";
        renderCurrentResults();
    }

    function loadHeritageData() {
        $.ajax({
            url: "/api/resources/approved",
            method: "GET",
            success: function (response) {
                heritageData = Array.isArray(response) ? response.map(normalizeBackendResource) : [];
                persistResourceCache(heritageData);
                populateFilterOptions(heritageData);
                handleInitialRender();
                showContent();
            },
            error: function () {
                heritageData = getFallbackResources();
                persistResourceCache(heritageData);
                populateFilterOptions(heritageData);
                handleInitialRender();
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

    $("#filter-toggle").click(function () {
        $("#filter-section").toggleClass("active");
    });

    $("#apply-filter").click(function () {
        applyFilters();
    });

    $("#search-btn").click(function () {
        applyFilters({ forceSearchRefresh: true });
    });

    $("#search-input").keypress(function (e) {
        if (e.which === 13) {
            applyFilters({ forceSearchRefresh: true });
        }
    });

    $(document).on("click", ".heritage-card", function () {
        const id = $(this).data("id");
        window.location.href = `detail.html?id=${id}`;
    });

    initPage();
});
