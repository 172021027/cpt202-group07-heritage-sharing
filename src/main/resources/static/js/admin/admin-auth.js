(function () {
    var authHeaderInstalled = false;
    var flashKey = 'adminFlashMessage';

    function getToken() {
        if (window.AuthState) return window.AuthState.getToken();
        return localStorage.getItem('token');
    }

    function safeParseUser() {
        if (window.AuthState) return window.AuthState.getStoredUser();
        var raw = localStorage.getItem('user');
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (e) {
            return null;
        }
    }

    function normalizeRole(role) {
        if (window.AuthState) return window.AuthState.normalizeRole(role);
        return String(role || '').trim().toLowerCase();
    }

    function isAdminRole(role) {
        if (window.AuthState) return window.AuthState.isAdminRole(role);
        var normalized = normalizeRole(role);
        return normalized === 'admin';
    }

    function getLoginUrl() {
        return window.location.pathname.indexOf('/admin/') >= 0 ? '../login.html' : 'login.html';
    }

    function clearAuth() {
        if (window.AuthState) {
            window.AuthState.clearAuth();
            return;
        }
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }

    function redirectToLogin() {
        if (window.AuthState) {
            window.AuthState.redirectToLogin();
            return;
        }
        window.location.href = getLoginUrl();
    }

    function ensureFlashHost() {
        var existing = document.getElementById('admin-flash');
        if (existing) return existing;

        var host = document.createElement('div');
        host.id = 'admin-flash';
        host.className = 'admin-flash';
        host.hidden = true;
        host.innerHTML = ''
            + '<div class="admin-flash__alert" role="alert" aria-live="polite">'
            + '<span class="admin-flash__icon" aria-hidden="true"></span>'
            + '<div class="admin-flash__body">'
            + '<div class="admin-flash__title"></div>'
            + '<div class="admin-flash__message"></div>'
            + '</div>'
            + '</div>';

        var main = document.querySelector('main');
        if (main && main.parentNode) {
            main.parentNode.insertBefore(host, main);
            return host;
        }

        document.body.insertBefore(host, document.body.firstChild);
        return host;
    }

    function showFlashMessage(type, message) {
        if (!message) return;

        var host = ensureFlashHost();
        var normalizedType = type === 'error' ? 'error' : 'success';
        var titleNode = host.querySelector('.admin-flash__title');
        var messageNode = host.querySelector('.admin-flash__message');

        host.className = 'admin-flash ' + normalizedType;
        if (titleNode) {
            titleNode.textContent = normalizedType === 'error' ? 'Action failed' : 'Action completed';
        }
        if (messageNode) {
            messageNode.textContent = String(message);
        }
        host.hidden = false;
    }

    function clearFlashMessage() {
        var host = document.getElementById('admin-flash');
        if (!host) return;
        var titleNode = host.querySelector('.admin-flash__title');
        var messageNode = host.querySelector('.admin-flash__message');
        if (titleNode) {
            titleNode.textContent = '';
        }
        if (messageNode) {
            messageNode.textContent = '';
        }
        host.hidden = true;
        host.className = 'admin-flash';
    }

    function pushFlash(type, message) {
        if (!message) return;
        sessionStorage.setItem(flashKey, JSON.stringify({
            type: type === 'error' ? 'error' : 'success',
            message: String(message)
        }));
    }

    function consumeFlash() {
        var raw = sessionStorage.getItem(flashKey);
        if (!raw) return null;

        sessionStorage.removeItem(flashKey);
        try {
            var flash = JSON.parse(raw);
            if (!flash || !flash.message) return null;
            return flash;
        } catch (e) {
            return null;
        }
    }

    function confirmAction(options) {
        if (window.AuthState && typeof window.AuthState.confirmAction === 'function') {
            return window.AuthState.confirmAction(options);
        }

        var deferred = $.Deferred();
        var message = options && options.message ? String(options.message) : 'Are you sure you want to continue?';
        if (window.confirm(message)) {
            deferred.resolve();
        } else {
            deferred.reject();
        }
        return deferred.promise();
    }

    function installApiAuthHeader() {
        if (window.AuthState) {
            window.AuthState.installAjaxAuthHeader();
            return;
        }
        if (authHeaderInstalled) return;
        if (!getToken() || !window.jQuery) return;

        // Inject Bearer token for same-origin /api requests.
        $.ajaxPrefilter(function (options, originalOptions, jqXHR) {
            var url = options.url || '';
            var isApi = url.indexOf('/api/') === 0 || url.indexOf('api/') === 0;
            if (!isApi) return;

            if (!options.headers) {
                options.headers = {};
            }
            if (!options.headers.Authorization) {
                var latestToken = getToken();
                if (latestToken) {
                    options.headers.Authorization = 'Bearer ' + latestToken;
                }
            }
        });

        authHeaderInstalled = true;
    }

    function ensureAdminOrRedirect(onPass) {
        if (window.AuthState) {
            window.AuthState.installAjaxAuthHeader();
            window.AuthState.requireRole(['admin'], {
                onPass: function () {
                    if (typeof onPass === 'function') {
                        onPass();
                    }
                },
                onForbidden: function () {
                    clearAuth();
                    redirectToLogin();
                }
            });
            return;
        }

        var token = getToken();
        var user = safeParseUser();
        if (!token || !user || !isAdminRole(user.role)) {
            clearAuth();
            redirectToLogin();
            return;
        }

        installApiAuthHeader();

        // Optional server-side verification: token validity + current role.
        $.ajax({
            url: '/api/users/current',
            type: 'GET',
            headers: {
                Authorization: 'Bearer ' + token
            }
        }).done(function (currentUser) {
            var verifiedRole = currentUser && currentUser.role ? currentUser.role : user.role;
            if (!currentUser || !isAdminRole(verifiedRole)) {
                clearAuth();
                redirectToLogin();
                return;
            }

            // Keep local cache fresh while preserving essential auth fields.
            var mergedUser = $.extend({}, user, currentUser);
            if (!mergedUser.role) {
                mergedUser.role = user.role;
            }
            localStorage.setItem('user', JSON.stringify(mergedUser));
            if (typeof onPass === 'function') {
                onPass();
            }
        }).fail(function () {
            clearAuth();
            redirectToLogin();
        });
    }

    window.AdminCommon = {
        ensureAdminOrRedirect: ensureAdminOrRedirect,
        confirmAction: confirmAction,
        showFlashMessage: showFlashMessage,
        clearFlashMessage: clearFlashMessage,
        pushFlash: pushFlash,
        consumeFlash: consumeFlash
    };
})();
