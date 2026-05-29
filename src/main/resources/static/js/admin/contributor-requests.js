$(function () {
    const API_BASE = "/api/admin/contributor-requests";

    function escapeHtml(text) {
        return String(text || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function showContent() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    function showMessage(type, message) {
        if (window.AdminCommon) {
            window.AdminCommon.showFlashMessage(type, message);
            return;
        }
        alert(message);
    }

    function renderRows(selector, users, emptyText, actionsBuilder) {
        const $body = $(selector);
        $body.empty();

        if (!users.length) {
            $body.append('<tr><td colspan="4" class="muted">' + escapeHtml(emptyText) + '</td></tr>');
            return;
        }

        users.forEach(function (user) {
            $body.append(
                '<tr>'
                + '<td>' + escapeHtml(user.username) + '</td>'
                + '<td>' + escapeHtml(user.email) + '</td>'
                + '<td><span class="badge approved">' + escapeHtml(user.role) + '</span></td>'
                + '<td><div class="admin-actions">' + actionsBuilder(user) + '</div></td>'
                + '</tr>'
            );
        });
    }

    function renderPending(users) {
        renderRows("#pendingContributorBody", users, "No pending contributor applications.", function (user) {
            return '<button class="admin-btn primary js-approve-request" data-id="' + escapeHtml(user.id) + '">Approve</button>'
                + '<button class="admin-btn danger js-reject-request" data-id="' + escapeHtml(user.id) + '">Reject</button>';
        });
    }

    function renderContributors(users) {
        renderRows("#currentContributorBody", users, "No contributors found.", function (user) {
            return '<button class="admin-btn danger js-revoke-contributor" data-id="' + escapeHtml(user.id) + '">Cancel Qualification</button>';
        });
    }

    function loadContributorData(options) {
        var settings = $.extend({
            flash: null,
            preserveFlash: false
        }, options || {});

        if (window.AdminCommon && !settings.preserveFlash && !settings.flash) {
            window.AdminCommon.clearFlashMessage();
        }

        $.when(
            $.getJSON(API_BASE),
            $.getJSON(API_BASE + "/contributors")
        ).done(function (pendingResult, contributorsResult) {
            renderPending(pendingResult[0] || []);
            renderContributors(contributorsResult[0] || []);
            showContent();
            if (settings.flash) {
                showMessage(settings.flash.type, settings.flash.message);
            }
        }).fail(function () {
            renderPending([]);
            renderContributors([]);
            showContent();
            showMessage("error", settings.flash && settings.flash.type !== "error"
                ? settings.flash.message + " Contributor data could not be refreshed."
                : "Failed to load contributor data.");
        });
    }

    function decideRequest(userId, action, successMessage, errorMessage) {
        $.ajax({
            url: API_BASE + "/" + encodeURIComponent(userId) + "/" + action,
            type: "POST"
        }).done(function () {
            loadContributorData({
                flash: {
                    type: "success",
                    message: successMessage
                }
            });
        }).fail(function (xhr) {
            const message = xhr.responseJSON && xhr.responseJSON.message
                ? xhr.responseJSON.message
                : (xhr.responseText || errorMessage);
            showMessage("error", message);
        });
    }

    function confirmAndDecide(userId, action, options) {
        var confirmOptions = options && options.confirmOptions;
        var runDecision = function () {
            decideRequest(userId, action, options.successMessage, options.errorMessage);
        };

        if (!confirmOptions) {
            runDecision();
            return;
        }

        var confirmAction = window.AdminCommon && typeof window.AdminCommon.confirmAction === "function"
            ? window.AdminCommon.confirmAction(confirmOptions)
            : null;

        if (!confirmAction) {
            if (window.confirm(confirmOptions.message || "Are you sure you want to continue?")) {
                runDecision();
            }
            return;
        }

        confirmAction.done(runDecision);
    }

    $(document).on("click", ".js-approve-request", function () {
        confirmAndDecide($(this).data("id"), "approve", {
            successMessage: "Contributor request approved.",
            errorMessage: "Failed to approve contributor request.",
            confirmOptions: {
                title: "Approve contributor request?",
                message: "This user will immediately gain contributor access.",
                confirmText: "Approve",
                cancelText: "Cancel",
                confirmKind: "primary"
            }
        });
    });

    $(document).on("click", ".js-reject-request", function () {
        confirmAndDecide($(this).data("id"), "reject", {
            successMessage: "Contributor request rejected.",
            errorMessage: "Failed to reject contributor request.",
            confirmOptions: {
                title: "Reject contributor request?",
                message: "This application will be rejected and the user can apply again later.",
                confirmText: "Reject",
                cancelText: "Keep pending",
                confirmKind: "danger"
            }
        });
    });

    $(document).on("click", ".js-revoke-contributor", function () {
        var userId = $(this).data("id");
        confirmAndDecide(userId, "revoke", {
            successMessage: "Contributor qualification cancelled.",
            errorMessage: "Failed to cancel contributor qualification.",
            confirmOptions: {
                title: "Cancel contributor qualification?",
                message: "This user will lose contributor access and can apply again later.",
                confirmText: "Cancel qualification",
                cancelText: "Keep qualification",
                confirmKind: "danger"
            }
        });
    });

    if (window.AdminCommon && typeof window.AdminCommon.ensureAdminOrRedirect === "function") {
        window.AdminCommon.ensureAdminOrRedirect(loadContributorData);
    } else {
        loadContributorData();
    }
});
