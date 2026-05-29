$(function () {
    const defaultAvatarUrl = "/static/images/default-avatar.svg";
    let isEditing = false;
    let isSaving = false;
    let isSubmittingContributorRequest = false;
    let selectedAvatarFile = null;
    let currentUser = null;

    $("#pageContent").hide();
    $("#pageLoading").show();

    function normalizeRole(role) {
        if (window.AuthState) return window.AuthState.normalizeRole(role);
        return String(role || "").trim().toLowerCase();
    }

    function normalizeStatus(status) {
        if (window.AuthState) return window.AuthState.normalizeRequestStatus(status);
        return String(status || "none").trim().toLowerCase();
    }

    function getToken() {
        return window.AuthState ? window.AuthState.getToken() : localStorage.getItem("token");
    }

    function redirectToLogin() {
        if (window.AuthState) window.AuthState.redirectToLogin();
        else window.location.href = "login.html";
    }

    function clearAuth() {
        if (window.AuthState) {
            window.AuthState.clearAuth();
            return;
        }
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    }

    function updateStoredUser(fields) {
        if (window.AuthState) {
            currentUser = window.AuthState.updateStoredUser(fields);
            return currentUser;
        }

        const raw = localStorage.getItem("user");
        let stored = {};
        if (raw) {
            try {
                stored = JSON.parse(raw);
            } catch (error) {
                stored = {};
            }
        }

        currentUser = $.extend({}, stored, fields);
        localStorage.setItem("user", JSON.stringify(currentUser));
        return currentUser;
    }

    function normalizeAvatarUrl(url) {
        const trimmed = String(url || "").trim();
        if (!trimmed || trimmed === "null" || trimmed === "undefined") {
            return defaultAvatarUrl;
        }
        return trimmed;
    }

    function roleLabel(role) {
        const normalized = normalizeRole(role);
        if (normalized === "contributor") return "contributor";
        return "user";
    }

    function updateProfileIdentity(user) {
        const name = String(user && user.username || "Username").trim() || "Username";
        let id = user && user.id ? String(user.id).padStart(8, '0') : "-";
        $("#profileIdentity").text(name + "#" + id);
    }

    function updateContributorRequestPanel(user) {
        const role = normalizeRole(user && user.role);
        const status = normalizeStatus(user && user.roleRequestStatus);
        const $button = $("#applyContributorBtn");

        $("#profileRole").text(roleLabel(role));

        if (role === "contributor") {
            $("#contributorRequestHint").text("You are approved as a contributor and can submit new resources.");
            $button.data("locked", true).prop("disabled", true).text("Approved");
            return;
        }

        if (status === "pending") {
            $("#contributorRequestHint").text("Your contributor request is waiting for admin approval.");
            $button.data("locked", true).prop("disabled", true).text("Pending");
            return;
        }

        if (status === "rejected") {
            $("#contributorRequestHint").text("Your previous request was rejected. You can apply again.");
            $button.data("locked", false).prop("disabled", false).text("Apply again");
            return;
        }

        if (status === "revoked") {
            $("#contributorRequestHint").text("Your contributor qualification was cancelled by an administrator. You can apply again.");
            $button.data("locked", false).prop("disabled", false).text("Apply again");
            return;
        }

        $("#contributorRequestHint").text("Apply to submit heritage resources for admin review.");
        $button.data("locked", false).prop("disabled", false).text("Apply");
    }

    function setLoaded() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    function handleAuthError(xhr) {
        if (xhr && (xhr.status === 401 || xhr.status === 403)) {
            clearAuth();
            redirectToLogin();
            return true;
        }
        return false;
    }

    function fillProfile(user) {
        currentUser = updateStoredUser(user);

        $("#profileUsername").val(user.username || "");
        $("#profileGender").val(user.gender || "");
        $("#profileBio").val(user.personalDescription || "");
        $("#profileAvatar").attr("src", normalizeAvatarUrl(user.profilePictureUrl || user.profilePicturePath));
        updateProfileIdentity(user);
        updateContributorRequestPanel(user);
    }

    function loadProfile() {
        const token = getToken();
        if (!token) {
            redirectToLogin();
            return;
        }

        $.ajax({
            url: "/api/users/current",
            type: "GET",
            headers: {
                Authorization: "Bearer " + token
            }
        }).done(function (user) {
            if (window.AuthState && window.AuthState.isAdminRole(user.role)) {
                window.AuthState.updateStoredUser(user);
                window.location.href = "admin.html";
                return;
            }
            fillProfile(user);
            setLoaded();
        }).fail(function (xhr) {
            if (!handleAuthError(xhr)) {
                setLoaded();
                alert("Failed to load profile.");
            }
        });
    }

    function setEditing(editing) {
        isEditing = editing;
        $("#profileUsername, #profileGender, #profileBio").prop("disabled", !editing);
        $("#editBtn").toggle(!editing);
        $("#saveBtn").toggle(editing);
    }

    function setSaving(saving) {
        isSaving = saving;
        $("#saveBtn").prop("disabled", saving).text(saving ? "Saving..." : "Save");
        $("#editBtn").prop("disabled", saving);
    }

    function currentProfilePayload() {
        return {
            username: $("#profileUsername").val().trim(),
            gender: $("#profileGender").val(),
            personalDescription: $("#profileBio").val().trim()
        };
    }

    function validateProfile(payload) {
        let valid = true;
        $("#usernameError").text("");
        $("#bioError").text("");

        if (!payload.username) {
            $("#usernameError").text("Username is required.");
            valid = false;
        } else if (payload.username.length > 50) {
            $("#usernameError").text("Username must be 50 characters or less.");
            valid = false;
        }

        if (payload.personalDescription.length > 500) {
            $("#bioError").text("Personal description must be 500 characters or less.");
            valid = false;
        }

        return valid;
    }

    function uploadAvatar(token) {
        if (!selectedAvatarFile) {
            return $.Deferred().resolve({}).promise();
        }

        const formData = new FormData();
        formData.append("avatar", selectedAvatarFile);

        return $.ajax({
            url: "/api/users/current/avatar",
            type: "POST",
            headers: {
                Authorization: "Bearer " + token
            },
            data: formData,
            processData: false,
            contentType: false
        });
    }

    function fetchCurrentUserProfile(token, options) {
        const settings = $.extend({
            syncPanel: true
        }, options || {});

        return $.ajax({
            url: "/api/users/current",
            type: "GET",
            cache: false,
            data: {
                _ts: Date.now()
            },
            headers: {
                Authorization: "Bearer " + token
            }
        }).done(function (user) {
            currentUser = updateStoredUser(user || {});
            if (settings.syncPanel) {
                updateContributorRequestPanel(currentUser);
            }
        });
    }

    function confirmContributorRequest() {
        if (window.AuthState && typeof window.AuthState.confirmAction === "function") {
            return window.AuthState.confirmAction({
                title: "Apply for contributor access?",
                message: "Your request will be submitted for admin review.",
                confirmText: "Submit request",
                cancelText: "Cancel",
                confirmKind: "primary"
            });
        }

        const deferred = $.Deferred();
        if (window.confirm("Submit contributor request for admin review?")) {
            deferred.resolve();
        } else {
            deferred.reject();
        }
        return deferred.promise();
    }

    function setContributorSubmittingState(submitting) {
        isSubmittingContributorRequest = submitting;
        if (submitting) {
            $("#applyContributorBtn").prop("disabled", true).text("Submitting...");
            return;
        }
        updateContributorRequestPanel(currentUser || {});
    }

    $("#profileAvatar").on("error", function () {
        if ($(this).attr("src") !== defaultAvatarUrl) {
            $(this).attr("src", defaultAvatarUrl);
        }
    });

    $("#editBtn").on("click", function () {
        setEditing(true);
    });

    $("#profileAvatar").on("click", function () {
        if (isEditing) {
            $("#avatarInput").click();
        }
    });

    $("#avatarInput").on("change", function () {
        const file = this.files && this.files[0];
        if (!file) return;

        selectedAvatarFile = file;
        const reader = new FileReader();
        reader.onload = function (event) {
            $("#profileAvatar").attr("src", event.target.result);
        };
        reader.readAsDataURL(file);
    });

    $("#saveBtn").on("click", function () {
        if (isSaving) return;

        const token = getToken();
        if (!token) {
            redirectToLogin();
            return;
        }

        const payload = currentProfilePayload();
        if (!validateProfile(payload)) {
            return;
        }

        setSaving(true);
        $.ajax({
            url: "/api/users/current",
            type: "PUT",
            headers: {
                Authorization: "Bearer " + token
            },
            contentType: "application/json",
            data: JSON.stringify(payload)
        }).then(function () {
            return uploadAvatar(token);
        }).done(function (avatarResponse) {
            currentUser = updateStoredUser($.extend({}, currentUser || {}, payload, avatarResponse || {}));
            fillProfile(currentUser);
            selectedAvatarFile = null;
            $("#avatarInput").val("");
            setEditing(false);
            alert("Profile saved successfully.");
        }).fail(function (xhr) {
            if (!handleAuthError(xhr)) {
                alert("Could not save profile.");
            }
        }).always(function () {
            setSaving(false);
        });
    });

    $("#applyContributorBtn").on("click", function () {
        if (isSubmittingContributorRequest) return;

        const token = getToken();
        if (!token) {
            redirectToLogin();
            return;
        }

        confirmContributorRequest().done(function () {
            setContributorSubmittingState(true);

            fetchCurrentUserProfile(token, { syncPanel: false }).then(function (latestUser) {
                const role = normalizeRole(latestUser && latestUser.role);
                const status = normalizeStatus(latestUser && latestUser.roleRequestStatus);

                if (role === "contributor") {
                    alert("You are already approved as a contributor.");
                    return $.Deferred().reject({ skipped: true }).promise();
                }

                if (status === "pending") {
                    alert("Your contributor request is already pending admin approval.");
                    return $.Deferred().reject({ skipped: true }).promise();
                }

                return $.ajax({
                    url: "/api/users/current/contributor-request",
                    type: "POST",
                    headers: {
                        Authorization: "Bearer " + token
                    }
                });
            }).done(function (response) {
                const message = (response && response.message) || "Contributor request submitted.";
                const optimisticUser = updateStoredUser($.extend({}, currentUser || {}, {
                    roleRequestStatus: "pending"
                }));
                updateContributorRequestPanel(optimisticUser);
                alert(message);
            }).fail(function (xhr) {
                if (xhr && xhr.skipped) {
                    return;
                }

                if (!handleAuthError(xhr)) {
                    if (xhr && (xhr.status === 400 || xhr.status === 409)) {
                        fetchCurrentUserProfile(token, { syncPanel: true }).always(function () {
                            const message = (xhr.responseJSON && xhr.responseJSON.message)
                                || xhr.responseText
                                || "Your request status changed. Please check and try again later.";
                            alert(message);
                        });
                        return;
                    }

                    alert((xhr && (xhr.responseText || (xhr.responseJSON && xhr.responseJSON.message)))
                        || "Could not submit contributor request.");
                }
            }).always(function () {
                setContributorSubmittingState(false);
            });
        });
    });

    loadProfile();
});
