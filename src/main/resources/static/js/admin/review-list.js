$(function () {
    var currentPage = 0;
    var pageSize = 10;
    var latestPageMeta = null;
    var pendingResources = [];

    function showContent() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    function startPage() {
        if (window.AdminCommon && typeof window.AdminCommon.clearFlashMessage === "function") {
            window.AdminCommon.clearFlashMessage();
        }
        if (window.AdminCommon && typeof window.AdminCommon.consumeFlash === "function") {
            var flash = window.AdminCommon.consumeFlash();
            if (flash && typeof window.AdminCommon.showFlashMessage === "function") {
                window.AdminCommon.showFlashMessage(flash.type, flash.message);
            }
        }
        loadPendingReviews();
    }

    if (window.AdminCommon && typeof window.AdminCommon.ensureAdminOrRedirect === "function") {
        window.AdminCommon.ensureAdminOrRedirect(startPage);
    } else {
        startPage();
    }

    function badgeClass(status) {
        if (status === "pending_review") return "badge pending";
        if (status === "approved") return "badge approved";
        return "badge";
    }

    function buildDetailUrl(resource, submissionId) {
        return "review-detail.html?resourceId=" + encodeURIComponent(resource.resourceId)
            + "&submissionId=" + encodeURIComponent(submissionId || "");
    }

    function render() {
        var list = pendingResources || [];
        var $tbody = $("#review-tbody");
        $tbody.empty();

        if (!list.length) {
            $tbody.append('<tr><td colspan="4" class="muted">No pending review items.</td></tr>');
            renderPagination();
            return;
        }

        list.forEach(function (resource) {
            var latest = (resource.submissions && resource.submissions.length)
                ? resource.submissions[resource.submissions.length - 1]
                : null;
            var submissionId = latest ? latest.submissionId : "";
            var url = buildDetailUrl(resource, submissionId);

            $tbody.append(
                '<tr data-resource-id="' + resource.resourceId + '">'
                + "<td>" + resource.title + "</td>"
                + '<td class="muted">' + resource.category + "</td>"
                + '<td><span class="' + badgeClass(resource.status) + '">' + resource.status + "</span></td>"
                + '<td><div class="admin-actions">'
                + '<a class="admin-btn primary js-review" href="' + url + '">Review</a>'
                + "</div></td>"
                + "</tr>"
            );
        });

        renderPagination();
    }

    function renderPagination() {
        var $pagination = $("#review-pagination");
        $pagination.empty();
        if (!latestPageMeta || latestPageMeta.totalPages <= 1) {
            return;
        }

        if (latestPageMeta.hasPrevious) {
            var $prevBtn = $('<button type="button" class="pagination-btn">Previous</button>');
            $prevBtn.on("click", function () {
                currentPage = Math.max(0, currentPage - 1);
                loadPendingReviews();
            });
            $pagination.append($prevBtn);
        } else {
            $pagination.append('<button type="button" class="pagination-btn" disabled>Previous</button>');
        }

        $pagination.append('<span style="padding: 0.5rem 1rem;">Page '
            + (latestPageMeta.currentPage + 1) + " of " + latestPageMeta.totalPages + "</span>");

        if (latestPageMeta.hasNext) {
            var $nextBtn = $('<button type="button" class="pagination-btn">Next</button>');
            $nextBtn.on("click", function () {
                currentPage += 1;
                loadPendingReviews();
            });
            $pagination.append($nextBtn);
        } else {
            $pagination.append('<button type="button" class="pagination-btn" disabled>Next</button>');
        }
    }

    function loadPendingReviews() {
        var q = $("#review-search").val().trim();
        var params = { page: currentPage, size: pageSize };
        if (q) {
            params.q = q;
        }

        $.getJSON("/api/admin/reviews/pending", params)
            .done(function (data) {
                pendingResources = (data && data.items) ? data.items : [];
                latestPageMeta = {
                    currentPage: (data && typeof data.currentPage === "number") ? data.currentPage : currentPage,
                    totalPages: (data && typeof data.totalPages === "number") ? data.totalPages : 0,
                    hasNext: !!(data && data.hasNext),
                    hasPrevious: !!(data && data.hasPrevious)
                };

                if (latestPageMeta.totalPages > 0 && latestPageMeta.currentPage >= latestPageMeta.totalPages) {
                    currentPage = latestPageMeta.totalPages - 1;
                    loadPendingReviews();
                    return;
                }

                render();
                showContent();
            })
            .fail(function () {
                pendingResources = [];
                latestPageMeta = null;
                render();
                showContent();
                if (window.AdminCommon && typeof window.AdminCommon.showFlashMessage === "function") {
                    window.AdminCommon.showFlashMessage("error", "Failed to load pending review items.");
                    return;
                }
                alert("Failed to load pending review items.");
            });
    }

    var searchTimer = null;
    $("#review-search").on("input", function () {
        if (searchTimer) {
            clearTimeout(searchTimer);
        }
        searchTimer = setTimeout(function () {
            currentPage = 0;
            loadPendingReviews();
        }, 250);
    });
});
