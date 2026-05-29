let works = [];
let currentUserRole = "";
let currentUsername = "";

function normalizeRole(role) {
    if (window.AuthState) return window.AuthState.normalizeRole(role);
    return String(role || "").trim().toLowerCase();
}

function isContributorRole(role) {
    if (window.AuthState) return window.AuthState.isContributorRole(role);
    return normalizeRole(role) === "contributor";
}

function getStoredUser() {
    if (window.AuthState) return window.AuthState.getStoredUser();
    const userStr = localStorage.getItem("user");
    if (!userStr) {
        return null;
    }

    try {
        return JSON.parse(userStr);
    } catch (error) {
        console.error("Failed to parse user info:", error);
        return null;
    }
}

function canSubmitNewResources() {
    return isContributorRole(currentUserRole || ((getStoredUser() || {}).role));
}

function updateNewSubmissionButton() {
    const $btn = $("#newSubmissionBtn");
    if (canSubmitNewResources()) {
        $btn.removeClass("disabled").attr("href", "submission.html").attr("aria-disabled", "false")
            .attr("title", "Submit a new resource");
    } else {
        $btn.addClass("disabled").attr("href", "#").attr("aria-disabled", "true")
            .attr("title", "Only contributors can submit new resources");
    }
}

function normalizeStatus(status) {
    const normalized = String(status || "")
        .trim()
        .toLowerCase()
        .replace(/[\s-]+/g, "_");

    if (!normalized || normalized === "pending" || normalized === "pending_review") return "pending";
    if (normalized === "approved") return "approved";
    if (normalized === "rejected") return "rejected";
    if (normalized === "archived") return "archived";
    if (normalized === "unpublished" || normalized === "offline") return "unpublished";
    if (normalized === "draft") return "draft";
    if (normalized === "deleted") return "deleted";
    return normalized;
}

function statusLabel(statusKey) {
    if (statusKey === "pending") return "Pending Review";
    if (statusKey === "approved") return "Approved";
    if (statusKey === "rejected") return "Rejected";
    if (statusKey === "archived") return "Archived";
    if (statusKey === "unpublished") return "Unpublished";
    if (statusKey === "draft") return "Draft";
    if (statusKey === "deleted") return "Deleted";
    return statusKey;
}

function statusBadgeClass(statusKey) {
    if (statusKey === "pending") return "badge pending";
    if (statusKey === "approved") return "badge approved";
    if (statusKey === "unpublished") return "badge unpublished";
    return "badge archived";
}

function escapeHtml(text) {
    return String(text || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function normalizeWork(item) {
    const normalizedStatus = normalizeStatus(item.status);
    return {
        id: item.resourceId,
        title: item.title || "Untitled",
        description: item.description || "No description provided.",
        submissionDate: item.submissionDate || "",
        status: normalizedStatus,
        statusText: statusLabel(normalizedStatus),
        feedback: item.feedback || "",
        reviewerId: item.reviewerId || "",
        reviewedAt: item.reviewedAt || ""
    };
}

function actionHtml(work) {
    if (work.status !== "rejected") {
        return '<span class="muted">-</span>';
    }

    if (!canSubmitNewResources()) {
        return '<span class="muted">Read only</span>';
    }

    return '<a class="admin-btn primary" href="submission.html?mode=revise&resourceId='
        + encodeURIComponent(work.id)
        + '">Revise</a>';
}

function dashboardEmptyMessage() {
    if (works.length === 0) {
        return "You have no submissions yet.";
    }
    return "Try changing the filter to find a matching submission.";
}

function updateStats() {
    const pending = works.filter(function (work) {
        return work.status === "pending";
    }).length;
    const approved = works.filter(function (work) {
        return work.status === "approved";
    }).length;
    const rejected = works.filter(function (work) {
        return work.status === "rejected";
    }).length;

    $("#pendingCount").text(pending);
    $("#approvedCount").text(approved);
    $("#rejectedCount").text(rejected);
}

function renderWorks() {
    updateNewSubmissionButton();
    updateStats();

    const keyword = $("#searchInput").val().trim().toLowerCase();
    const filter = $("#statusFilter").val();

    const filtered = works.filter(function (work) {
        const matchesKeyword = !keyword || work.title.toLowerCase().includes(keyword);
        const matchesStatus =
            filter === "all" ? true :
                filter === "pending" ? work.status === "pending" :
                    filter === "approved" ? work.status === "approved" :
                        work.status === "rejected";

        return matchesKeyword && matchesStatus;
    });

    if (filtered.length === 0) {
        $("#myWorksContainer").html(
            '<div class="myworks-empty">'
            + '<h3>No submitted works found.</h3>'
            + "<p>" + escapeHtml(dashboardEmptyMessage()) + "</p>"
            + "</div>"
        );
        return;
    }

    let rows = "";
    filtered.forEach(function (work) {
        rows += ""
            + "<tr>"
            + '<td data-label="Work">'
            + '<div class="myworks-title">' + escapeHtml(work.title) + "</div>"
            + '<div class="myworks-meta">' + escapeHtml(work.description) + "</div>"
            + "</td>"
            + '<td data-label="Submitted At">' + escapeHtml(work.submissionDate) + "</td>"
            + '<td data-label="Status">'
            + '<span class="' + statusBadgeClass(work.status) + '">' + escapeHtml(work.statusText) + "</span>"
            + (work.status === "rejected" && work.feedback
                ? '<div class="myworks-feedback-box"><b>Reviewer Feedback:</b><br>'
                + escapeHtml(work.feedback)
                + "</div>"
                : "")
            + ((work.reviewerId || work.reviewedAt)
                ? '<div class="myworks-meta"><b>Reviewer:</b> '
                + escapeHtml(work.reviewerId || "-")
                + "<br><b>Reviewed At:</b> "
                + escapeHtml(work.reviewedAt || "-")
                + "</div>"
                : "")
            + "</td>"
            + '<td data-label="Actions">' + actionHtml(work) + "</td>"
            + "</tr>";
    });

    $("#myWorksContainer").html(
        '<div class="myworks-table-wrap">'
        + '<table class="myworks-table">'
        + "<thead><tr><th>Work</th><th>Submitted At</th><th>Status</th><th>Actions</th></tr></thead>"
        + "<tbody>" + rows + "</tbody>"
        + "</table>"
        + "</div>"
    );
}

function handleUnauthorized(xhr) {
    if (xhr && (xhr.status === 401 || xhr.status === 403)) {
        if (window.AuthState) {
            window.AuthState.clearAuth();
            window.AuthState.redirectToLogin();
        } else {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            window.location.href = "login.html";
        }
    }
}

function setDashboardLoaded() {
    $(".dashboard-main").removeClass("is-loading").removeAttr("aria-busy");
    $("#pageLoading").hide();
    $("#pageContent").show();
}

function loadUserInfo() {
    return $.ajax({
        url: "/api/users/current",
        type: "GET",
        headers: {
            Authorization: "Bearer " + (window.AuthState ? window.AuthState.getToken() : localStorage.getItem("token"))
        }
    }).done(function (response) {
        if (!response || !response.username) {
            return;
        }

        currentUserRole = normalizeRole(response.role);
        currentUsername = response.username || "";

        if (window.AuthState && window.AuthState.isAdminRole(response.role)) {
            window.AuthState.updateStoredUser(response);
            window.location.href = "admin.html";
            return;
        }

        if (window.AuthState) {
            window.AuthState.updateStoredUser(response);
        } else {
            localStorage.setItem("user", JSON.stringify(response));
        }

        $("#contributorBadge").text(currentUsername ? ("User: " + currentUsername) : "User");
        updateNewSubmissionButton();
    }).fail(function (xhr) {
        handleUnauthorized(xhr);
    });
}

function shouldLoadDashboardSubmissions() {
    return !window.AuthState || !window.AuthState.isAdminRole(currentUserRole);
}

function isAuthFailure(xhr) {
    return !!(xhr && (xhr.status === 401 || xhr.status === 403));
}

function loadMySubmissions() {
    return $.ajax({
        url: "/api/users/current/submissions",
        type: "GET",
        headers: {
            Authorization: "Bearer " + (window.AuthState ? window.AuthState.getToken() : localStorage.getItem("token"))
        }
    }).done(function (response) {
        const list = Array.isArray(response) ? response : [];
        works = list.map(normalizeWork);
        renderWorks();
    }).fail(function (xhr) {
        handleUnauthorized(xhr);
        works = [];
        renderWorks();
    });
}

$(document).ready(function () {
    const token = window.AuthState ? window.AuthState.getToken() : localStorage.getItem("token");
    if (!token) {
        if (window.AuthState) window.AuthState.redirectToLogin();
        else window.location.href = "login.html";
        return;
    }

    currentUserRole = normalizeRole((getStoredUser() || {}).role);
    updateNewSubmissionButton();

    loadUserInfo()
        .done(function () {
            if (!shouldLoadDashboardSubmissions()) {
                setDashboardLoaded();
                return;
            }
            loadMySubmissions().always(function () {
                setDashboardLoaded();
            });
        })
        .fail(function (xhr) {
            if (isAuthFailure(xhr) || !shouldLoadDashboardSubmissions()) {
                return;
            }
            loadMySubmissions().always(function () {
                setDashboardLoaded();
            });
        });

    $("#searchInput, #statusFilter").on("input change", function () {
        renderWorks();
    });

    $("#newSubmissionBtn").on("click", function (event) {
        if (!canSubmitNewResources()) {
            event.preventDefault();
            alert("Only contributors can submit new resources. You can still view your past submissions here.");
        }
    });
});
