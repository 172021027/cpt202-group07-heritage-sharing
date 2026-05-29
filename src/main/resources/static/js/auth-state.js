(function () {
    var TOKEN_KEY = "token";
    var USER_KEY = "user";
    var authHeaderInstalled = false;
    var confirmDialog = null;
    var lastFocusedElement = null;
    var activeConfirmState = null;

    function normalizeTokenValue(value) {
        return String(value || "")
            .trim()
            .toLowerCase()
            .replace(/[\s-]+/g, "_");
    }

    function normalizeRole(role) {
        var normalized = normalizeTokenValue(role);
        if (normalized.indexOf("role_") === 0) {
            normalized = normalized.substring("role_".length);
        }
        return normalized;
    }

    function normalizeRequestStatus(status) {
        var normalized = normalizeTokenValue(status);
        return normalized || "none";
    }

    function normalizeUser(user) {
        if (!user) return null;
        var normalized = $.extend({}, user);
        normalized.role = normalizeRole(normalized.role);
        normalized.roleRequestStatus = normalizeRequestStatus(normalized.roleRequestStatus);
        return normalized;
    }

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function getStoredUser() {
        var raw = localStorage.getItem(USER_KEY);
        if (!raw) return null;
        try {
            return normalizeUser(JSON.parse(raw));
        } catch (error) {
            return null;
        }
    }

    function saveAuth(token, user) {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        }
        if (user) {
            localStorage.setItem(USER_KEY, JSON.stringify(normalizeUser(user)));
        }
    }

    function updateStoredUser(fields) {
        var current = getStoredUser() || {};
        var merged = normalizeUser($.extend({}, current, fields));
        localStorage.setItem(USER_KEY, JSON.stringify(merged));
        return merged;
    }

    function clearAuth() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }

    function getLoginUrl() {
        return window.location.pathname.indexOf("/admin/") >= 0 ? "../login.html" : "login.html";
    }

    function redirectToLogin() {
        window.location.href = getLoginUrl();
    }

    function isLogoutLink(link) {
        if (!link || link.tagName !== "A") return false;

        var authAction = String(link.getAttribute("data-auth-action") || "").trim().toLowerCase();
        if (authAction === "logout") {
            return true;
        }

        var href = String(link.getAttribute("href") || "").trim().toLowerCase();
        var text = String(link.textContent || "").trim().toLowerCase();
        return text === "logout" && (href === "login.html" || href === "../login.html");
    }

    function closeConfirmDialog() {
        if (!confirmDialog) return;

        confirmDialog.hidden = true;
        document.body.classList.remove("app-confirm-open");
        if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
            lastFocusedElement.focus();
        }
        lastFocusedElement = null;
        activeConfirmState = null;
    }

    function ensureConfirmDialog() {
        if (confirmDialog) return confirmDialog;
        if (!document.body) return null;

        var dialog = document.createElement("div");
        dialog.className = "app-confirm";
        dialog.hidden = true;
        dialog.innerHTML = ""
            + '<div class="app-confirm__backdrop" data-confirm-cancel="true"></div>'
            + '<div class="app-confirm__card" role="dialog" aria-modal="true" aria-labelledby="app-confirm-title" aria-describedby="app-confirm-message" tabindex="-1">'
            + '<h2 id="app-confirm-title"></h2>'
            + '<p id="app-confirm-message"></p>'
            + '<div class="app-confirm__actions">'
            + '<button type="button" class="app-confirm__btn app-confirm__btn--secondary" data-confirm-cancel="true">Cancel</button>'
            + '<button type="button" class="app-confirm__btn app-confirm__btn--danger" data-confirm-submit="true">Confirm</button>'
            + "</div>"
            + "</div>";

        dialog.addEventListener("click", function (event) {
            var target = event.target;
            if (target && target.getAttribute("data-confirm-cancel") === "true") {
                resolveConfirmAction(false);
                return;
            }

            if (target && target.getAttribute("data-confirm-submit") === "true") {
                resolveConfirmAction(true);
            }
        });

        dialog.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                event.preventDefault();
                resolveConfirmAction(false);
            }
        });

        document.body.appendChild(dialog);
        confirmDialog = dialog;
        return confirmDialog;
    }

    function resolveConfirmAction(confirmed) {
        var state = activeConfirmState;
        if (!state) {
            closeConfirmDialog();
            return;
        }

        var deferred = state.deferred;
        closeConfirmDialog();

        if (confirmed) {
            deferred.resolve();
        } else {
            deferred.reject();
        }
    }

    function confirmAction(options) {
        var settings = $.extend({
            title: "Confirm action",
            message: "Are you sure you want to continue?",
            confirmText: "Confirm",
            cancelText: "Cancel",
            confirmKind: "danger"
        }, options || {});
        var deferred = $.Deferred();
        var dialog = ensureConfirmDialog();

        if (activeConfirmState && activeConfirmState.deferred) {
            activeConfirmState.deferred.reject();
            closeConfirmDialog();
        }

        if (!dialog) {
            if (window.confirm(settings.message)) {
                deferred.resolve();
            } else {
                deferred.reject();
            }
            return deferred.promise();
        }

        var titleNode = dialog.querySelector("#app-confirm-title");
        var messageNode = dialog.querySelector("#app-confirm-message");
        var cancelButton = dialog.querySelector("button[data-confirm-cancel='true']");
        var confirmButton = dialog.querySelector("button[data-confirm-submit='true']");
        var dialogCard = dialog.querySelector(".app-confirm__card");
        var confirmKind = settings.confirmKind === "primary" ? "primary" : "danger";

        if (titleNode) {
            titleNode.textContent = String(settings.title || "Confirm action");
        }
        if (messageNode) {
            messageNode.textContent = String(settings.message || "Are you sure you want to continue?");
        }
        if (cancelButton) {
            cancelButton.textContent = String(settings.cancelText || "Cancel");
        }
        if (confirmButton) {
            confirmButton.textContent = String(settings.confirmText || "Confirm");
            confirmButton.className = "app-confirm__btn app-confirm__btn--" + confirmKind;
        }

        activeConfirmState = {
            deferred: deferred
        };
        lastFocusedElement = document.activeElement;
        dialog.hidden = false;
        document.body.classList.add("app-confirm-open");

        if (cancelButton) {
            cancelButton.focus();
        } else if (dialogCard) {
            dialogCard.focus();
        }

        return deferred.promise();
    }

    function confirmLogout() {
        return confirmAction({
            title: "Log out?",
            message: "You will need to sign in again to continue.",
            confirmText: "Log out",
            cancelText: "Cancel",
            confirmKind: "danger"
        }).done(function () {
            clearAuth();
            redirectToLogin();
        });
    }

    function isAdminRole(role) {
        var normalized = normalizeRole(role);
        return normalized === "admin";
    }

    function isContributorRole(role) {
        return normalizeRole(role) === "contributor";
    }

    function hasAnyRole(user, roles) {
        if (!roles || roles.length === 0) return true;
        var role = normalizeRole(user && user.role);
        return roles.map(normalizeRole).indexOf(role) >= 0;
    }

    function installAjaxAuthHeader() {
        if (authHeaderInstalled || !window.jQuery) return;

        $.ajaxPrefilter(function (options) {
            var url = options.url || "";
            var isApi = url.indexOf("/api/") === 0 || url.indexOf("api/") === 0;
            if (!isApi) return;

            options.headers = options.headers || {};
            if (!options.headers.Authorization) {
                var latestToken = getToken();
                if (latestToken) {
                    options.headers.Authorization = "Bearer " + latestToken;
                }
            }
        });

        authHeaderInstalled = true;
    }

    function fetchCurrentUser() {
        var token = getToken();
        if (!token) {
            var deferred = $.Deferred();
            deferred.reject({ status: 401 });
            return deferred.promise();
        }

        installAjaxAuthHeader();
        return $.ajax({
            url: "/api/users/current",
            type: "GET",
            headers: {
                Authorization: "Bearer " + token
            }
        }).then(function (user) {
            var normalized = normalizeUser(user);
            updateStoredUser(normalized);
            return normalized;
        }, function (xhr) {
            if (xhr && (xhr.status === 401 || xhr.status === 403)) {
                clearAuth();
            }
            return $.Deferred().reject(xhr).promise();
        });
    }

    function requireAuthenticated(options) {
        options = options || {};
        var token = getToken();
        if (!token) {
            clearAuth();
            if (options.redirect !== false) {
                redirectToLogin();
            }
            if (typeof options.onFail === "function") {
                options.onFail({ status: 401 });
            }
            return $.Deferred().reject({ status: 401 }).promise();
        }

        return fetchCurrentUser().done(function (user) {
            if (!hasAnyRole(user, options.roles || [])) {
                if (typeof options.onForbidden === "function") {
                    options.onForbidden(user);
                } else if (options.forbiddenRedirect) {
                    window.location.href = options.forbiddenRedirect;
                } else if (options.redirect !== false) {
                    redirectToLogin();
                }
                return;
            }

            if (typeof options.onPass === "function") {
                options.onPass(user);
            }
        }).fail(function (xhr) {
            if (options.redirect !== false) {
                redirectToLogin();
            }
            if (typeof options.onFail === "function") {
                options.onFail(xhr);
            }
        });
    }

    function requireRole(roles, options) {
        options = options || {};
        options.roles = Array.isArray(roles) ? roles : [roles];
        return requireAuthenticated(options);
    }

    function redirectAfterLogin(user) {
        if (isAdminRole(user && user.role)) {
            window.location.href = "admin.html";
            return;
        }
        window.location.href = "search.html";
    }

    function bindLogoutLinks() {
        $(document).on("click", "a", function (event) {
            if (!isLogoutLink(this)) return;
            event.preventDefault();
            confirmLogout();
        });
    }

    if (window.jQuery) {
        $(function () {
            installAjaxAuthHeader();
            bindLogoutLinks();
        });
    }

    window.AuthState = {
        normalizeRole: normalizeRole,
        normalizeRequestStatus: normalizeRequestStatus,
        normalizeUser: normalizeUser,
        getToken: getToken,
        getStoredUser: getStoredUser,
        saveAuth: saveAuth,
        updateStoredUser: updateStoredUser,
        clearAuth: clearAuth,
        confirmAction: confirmAction,
        confirmLogout: confirmLogout,
        redirectToLogin: redirectToLogin,
        isAdminRole: isAdminRole,
        isContributorRole: isContributorRole,
        installAjaxAuthHeader: installAjaxAuthHeader,
        fetchCurrentUser: fetchCurrentUser,
        requireAuthenticated: requireAuthenticated,
        requireRole: requireRole,
        redirectAfterLogin: redirectAfterLogin
    };
})();
