$(function () {
    const API_BASE_URL = "/api";
    const queryParams = new URLSearchParams(window.location.search);
    const isReviseMode = queryParams.get("mode") === "revise";
    const reviseResourceId = queryParams.get("resourceId");

    let tags = [];
    let canSubmitAsContributor = false;
    let existingPicturePath = "";
    let existingVideoPath = "";
    let isSubmitting = false;

    $("#pageContent").hide();
    $("#pageLoading").show();

    function getToken() {
        return window.AuthState ? window.AuthState.getToken() : localStorage.getItem("token");
    }

    function redirectToLogin() {
        if (window.AuthState) {
            window.AuthState.redirectToLogin();
        } else {
            window.location.href = "login.html";
        }
    }

    function handleAuthError(xhr) {
        if (!xhr || (xhr.status !== 401 && xhr.status !== 403)) {
            return false;
        }
        if (window.AuthState) {
            window.AuthState.clearAuth();
            window.AuthState.redirectToLogin();
        } else {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            window.location.href = "login.html";
        }
        return true;
    }

    function normalizeRole(role) {
        if (window.AuthState) return window.AuthState.normalizeRole(role);
        return String(role || "").trim().toLowerCase();
    }

    function isContributorRole(role) {
        if (window.AuthState) return window.AuthState.isContributorRole(role);
        return normalizeRole(role) === "contributor";
    }

    function escapeHtml(text) {
        return String(text || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function updateStoredUser(user) {
        if (!user) return;
        if (window.AuthState) {
            window.AuthState.updateStoredUser(user);
            return;
        }
        localStorage.setItem("user", JSON.stringify(user));
    }

    function showContent() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    function setReviseModeUi() {
        if (!isReviseMode) {
            return;
        }
        $("#submissionTitle").text("Revise Resource");
        $("#submissionSubtitle").text("Update the rejected resource and resubmit it for review.");
        $("#addBtn").text("Resubmit for Review");
    }

    function showReviseMessage(message) {
        $("#reviseStatusMessage")
            .prop("hidden", false)
            .html(escapeHtml(message).replace(/\n/g, "<br>"));
    }

    function lockSubmissionForm(message) {
        canSubmitAsContributor = false;
        if (!$("#submissionAccessMessage").length) {
            $(".submission-form").prepend('<div class="admin-empty" id="submissionAccessMessage"></div>');
        }
        $("#submissionAccessMessage").text(message);
        $(".submission-form :input").prop("disabled", true);
        $("#backBtn").prop("disabled", false);
        $("#addBtn").prop("disabled", true).attr("title", message);
    }

    function getDraftStorageKey() {
        if (isReviseMode && reviseResourceId) {
            return "heritage_draft_revise_" + reviseResourceId;
        }
        return "heritage_draft";
    }

    function toAssetUrl(path) {
        const normalized = String(path || "").replace(/\\/g, "/").trim();
        if (!normalized) return "";
        if (/^(https?:|data:|blob:)/i.test(normalized)) return normalized;
        if (normalized.charAt(0) === "/") return normalized;
        return "/" + normalized.replace(/^\.?\//, "");
    }

    function selectedFile(inputId) {
        const input = $("#" + inputId)[0];
        return input && input.files && input.files.length ? input.files[0] : null;
    }

    function getFileExtension(file) {
        const name = file && file.name ? String(file.name).trim().toLowerCase() : "";
        const dotIndex = name.lastIndexOf(".");
        return dotIndex >= 0 ? name.substring(dotIndex + 1) : "";
    }

    function isExpectedMediaFile(file, isVideo) {
        if (!file) return true;

        const expectedType = isVideo ? "video/" : "image/";
        const allowedExtensions = isVideo
            ? ["mp4", "mov", "avi", "mkv", "webm", "mpeg", "mpg", "m4v"]
            : ["jpg", "jpeg", "png", "gif", "webp", "bmp", "avif", "svg"];
        const type = String(file.type || "").toLowerCase();
        if (type) {
            return type.indexOf(expectedType) === 0;
        }

        return allowedExtensions.indexOf(getFileExtension(file)) >= 0;
    }

    function renderExistingImagePreview() {
        if (!existingPicturePath) {
            $("#filePreviewContainer").empty();
            return;
        }
        const url = toAssetUrl(existingPicturePath);
        $("#filePreviewContainer").html(
            '<div class="img-preview-item">'
            + '<img src="' + escapeHtml(url) + '" alt="Current resource image">'
            + '<div class="myworks-meta">Current image will be kept unless replaced.</div>'
            + "</div>"
        );
    }

    function renderExistingVideoPreview() {
        if (!existingVideoPath) {
            $("#videoPreviewContainer").empty();
            return;
        }
        const url = toAssetUrl(existingVideoPath);
        $("#videoPreviewContainer").html(
            '<div class="img-preview-item">'
            + '<video src="' + escapeHtml(url) + '" controls></video>'
            + '<div class="myworks-meta">Current video will be kept unless replaced.</div>'
            + "</div>"
        );
    }

    function renderExistingMediaPreviews() {
        renderExistingImagePreview();
        renderExistingVideoPreview();
    }

    function renderTags() {
        $("#addedTagsContainer").html(tags.map(function (tag) {
            return '<span class="tag-chip-added">' + escapeHtml(tag)
                + '<span class="remove-tag" data-tag="' + escapeHtml(tag) + '">&times;</span></span>';
        }).join(""));
    }

    function updateCounters() {
        $("#nameCount").text($("#name").val().length + "/50");
        $("#descCount").text($("#desc").val().length + "/5000");
    }

    function checkFormValidity() {
        const hasImage = !!selectedFile("file") || (isReviseMode && !!existingPicturePath);
        const hasVideo = !!selectedFile("videoFile") || (isReviseMode && !!existingVideoPath);
        const isValid = canSubmitAsContributor
            && $("#name").val().trim().length > 0
            && $("#loc").val().trim().length > 0
            && $("#category").val()
            && tags.length > 0
            && $("#desc").val().trim().length > 0
            && hasImage
            && hasVideo
            && $("#agreeCheck").is(":checked")
            && !isSubmitting;

        $("#addBtn").prop("disabled", !isValid);
        return isValid;
    }

    function loadDraft() {
        const raw = localStorage.getItem(getDraftStorageKey());
        if (!raw) return;

        try {
            const draft = JSON.parse(raw);
            $("#name").val(draft.title || "");
            $("#loc").val(draft.location || "");
            $("#category").val(draft.categoryId || "");
            $("#desc").val(draft.description || "");
            tags = Array.isArray(draft.tags) ? draft.tags.slice(0, 3) : [];
            renderTags();
            updateCounters();
            checkFormValidity();
        } catch (error) {
            localStorage.removeItem(getDraftStorageKey());
        }
    }

    function saveDraft() {
        const draft = {
            title: $("#name").val(),
            location: $("#loc").val(),
            categoryId: $("#category").val(),
            description: $("#desc").val(),
            tags: tags
        };
        localStorage.setItem(getDraftStorageKey(), JSON.stringify(draft));
        $("#draftTip").stop(true, true).text("Saved!").fadeIn(150).delay(900).fadeOut(200);
    }

    function resetFormAfterSubmit() {
        localStorage.removeItem(getDraftStorageKey());
        $("#name, #loc, #desc").val("");
        $("#category, #tagSelect").val("");
        $("#file, #videoFile").val("");
        $("#agreeCheck").prop("checked", false);
        $("#filePreviewContainer, #videoPreviewContainer, #addedTagsContainer").empty();
        tags = [];
        existingPicturePath = "";
        existingVideoPath = "";
        updateCounters();
        checkFormValidity();
    }

    function renderPreview(inputId, containerSelector, isVideo) {
        const file = selectedFile(inputId);
        const $container = $(containerSelector);
        $container.empty();

        if (!file) {
            if (isReviseMode && inputId === "file") {
                renderExistingImagePreview();
            } else if (isReviseMode && inputId === "videoFile") {
                renderExistingVideoPreview();
            }
            checkFormValidity();
            return;
        }

        if (!isExpectedMediaFile(file, isVideo)) {
            alert("Invalid media upload. Please upload an image to the image field and a video to the video field.");
            $("#" + inputId).val("");
            if (isReviseMode && inputId === "file") {
                renderExistingImagePreview();
            } else if (isReviseMode && inputId === "videoFile") {
                renderExistingVideoPreview();
            }
            checkFormValidity();
            return;
        }

        const url = URL.createObjectURL(file);
        const media = isVideo
            ? '<video src="' + escapeHtml(url) + '" controls></video>'
            : '<img src="' + escapeHtml(url) + '" alt="Preview">';
        $container.html(
            '<div class="img-preview-item">' + media
            + '<button type="button" class="btn-del btn-remove-preview" data-target="' + inputId + '">&times;</button>'
            + "</div>"
        );
        checkFormValidity();
    }

    function finalizeStartup() {
        updateCounters();
        checkFormValidity();
        showContent();
    }

    function shouldContinueLoadingAfterAccessCheck() {
        return canSubmitAsContributor;
    }

    function verifyContributorAccess() {
        const token = getToken();
        if (!token) {
            redirectToLogin();
            return $.Deferred().reject({ status: 401 }).promise();
        }

        return $.ajax({
            url: API_BASE_URL + "/users/current",
            type: "GET",
            headers: {
                Authorization: "Bearer " + token
            }
        }).done(function (user) {
            updateStoredUser(user);
            if (!isContributorRole(user.role)) {
                lockSubmissionForm("Only Contributor users can submit resources. Please apply from your profile page.");
                return;
            }

            canSubmitAsContributor = true;
            $("#submissionAccessMessage").remove();
            $(".submission-form :input").prop("disabled", false);
            $("#backBtn").prop("disabled", false);
            checkFormValidity();
        }).fail(function (xhr) {
            if (!handleAuthError(xhr)) {
                lockSubmissionForm("Could not verify Contributor access. Please try again later.");
            }
        });
    }

    function loadCategories() {
        return $.ajax({
            url: API_BASE_URL + "/categories",
            type: "GET",
            headers: {
                Authorization: "Bearer " + getToken()
            }
        }).done(function (categories) {
            let options = '<option value="">Select Category...</option>';
            (categories || []).forEach(function (cat) {
                options += '<option value="' + escapeHtml(cat.categoryId) + '">'
                    + escapeHtml(cat.categoryName)
                    + "</option>";
            });
            $("#category").html(options);
        }).fail(function (xhr, status, error) {
            if (!handleAuthError(xhr)) {
                console.error("Failed to load categories:", status, error);
            }
        });
    }

    function loadTags() {
        return $.ajax({
            url: API_BASE_URL + "/tags",
            type: "GET",
            headers: {
                Authorization: "Bearer " + getToken()
            }
        }).done(function (tagList) {
            let options = '<option value="">Add a tag...</option>';
            (tagList || []).forEach(function (tag) {
                options += '<option value="' + escapeHtml(tag.tagName) + '">'
                    + escapeHtml(tag.tagName)
                    + "</option>";
            });
            $("#tagSelect").html(options);
        }).fail(function (xhr, status, error) {
            if (!handleAuthError(xhr)) {
                console.error("Failed to load tags:", status, error);
            }
        });
    }

    function loadRejectedSubmissionForEdit() {
        return $.ajax({
            url: API_BASE_URL + "/users/current/submissions/" + encodeURIComponent(reviseResourceId),
            type: "GET",
            headers: {
                Authorization: "Bearer " + getToken()
            }
        }).done(function (data) {
            $("#name").val(data.title || "");
            $("#loc").val(data.location || "");
            $("#category").val(data.categoryId == null ? "" : String(data.categoryId));
            $("#desc").val(data.description || "");
            $("#agreeCheck").prop("checked", true);

            tags = Array.isArray(data.tags) ? data.tags.slice(0, 3) : [];
            existingPicturePath = data.picturePath || "";
            existingVideoPath = data.videoPath || "";

            renderTags();
            renderExistingMediaPreviews();
            updateCounters();

            if (data.feedback) {
                showReviseMessage("Reviewer Feedback:\n" + data.feedback);
            }

            loadDraft();
            checkFormValidity();
        }).fail(function (xhr) {
            if (handleAuthError(xhr)) {
                return;
            }
            const message = (xhr.responseJSON && xhr.responseJSON.message)
                || xhr.responseText
                || "Could not load the rejected resource.";
            lockSubmissionForm(message);
        });
    }

    $("#backBtn").on("click", function () {
        window.location.href = "dashboard.html";
    });

    $(".validate-field, #agreeCheck").on("input change", function () {
        updateCounters();
        checkFormValidity();
    });

    $("#draftBtn").on("click", function () {
        saveDraft();
    });

    $("#file").on("change", function () {
        renderPreview("file", "#filePreviewContainer", false);
    });

    $("#videoFile").on("change", function () {
        renderPreview("videoFile", "#videoPreviewContainer", true);
    });

    $(document).on("click", ".btn-remove-preview", function () {
        const targetId = $(this).data("target");
        $("#" + targetId).val("");
        if (isReviseMode && targetId === "file") {
            renderExistingImagePreview();
        } else if (isReviseMode && targetId === "videoFile") {
            renderExistingVideoPreview();
        } else {
            $(this).closest(".img-preview-item").parent().empty();
        }
        checkFormValidity();
    });

    $("#tagSelect").on("change", function () {
        const selected = $(this).val();
        if (!selected) return;
        if (tags.length >= 3) {
            alert("You can add up to 3 tags.");
        } else if (tags.indexOf(selected) < 0) {
            tags.push(selected);
        }
        renderTags();
        $(this).val("");
        checkFormValidity();
    });

    $(document).on("click", ".remove-tag", function () {
        const tag = $(this).data("tag");
        tags = tags.filter(function (item) {
            return item !== tag;
        });
        renderTags();
        checkFormValidity();
    });

    $("#addBtn").on("click", function () {
        if (!checkFormValidity() || isSubmitting) return;

        const token = getToken();
        if (!token) {
            redirectToLogin();
            return;
        }

        isSubmitting = true;
        const formData = new FormData();
        formData.append("title", $("#name").val().trim());
        formData.append("location", $("#loc").val().trim());
        formData.append("categoryId", $("#category").val());
        formData.append("description", $("#desc").val().trim());
        formData.append("copyrightDeclaration", "User agreed to terms");
        tags.forEach(function (tag) {
            formData.append("tags", tag);
        });

        const image = selectedFile("file");
        const video = selectedFile("videoFile");
        if (image) formData.append("image", image);
        if (video) formData.append("video", video);

        const url = isReviseMode
            ? API_BASE_URL + "/users/current/submissions/" + encodeURIComponent(reviseResourceId) + "/resubmit"
            : API_BASE_URL + "/resources/submit";

        const $button = $(this);
        $button.prop("disabled", true).text("Processing...");

        $.ajax({
            url: url,
            type: "POST",
            headers: {
                Authorization: "Bearer " + token
            },
            data: formData,
            processData: false,
            contentType: false
        }).done(function (response) {
            if (response && response.success === false) {
                alert("Error: " + response.message);
                return;
            }

            if (isReviseMode) {
                localStorage.removeItem(getDraftStorageKey());
                alert("Resource resubmitted for review.");
                window.location.href = "dashboard.html";
                return;
            }

            alert("Success. Your resource has been submitted for review.");
            resetFormAfterSubmit();
        }).fail(function (xhr, status, error) {
            if (!handleAuthError(xhr)) {
                const message = (xhr.responseJSON && xhr.responseJSON.message) || xhr.responseText || error;
                alert("Error submitting resource: " + message);
            }
        }).always(function () {
            isSubmitting = false;
            $button.text(isReviseMode ? "Resubmit for Review" : "Submit Everything");
            checkFormValidity();
        });
    });

    $("#viewAgreement").on("click", function (event) {
        event.preventDefault();
        $("#agreementModal").css("display", "flex").hide().fadeIn(200);
    });

    $("#modalOk").on("click", function () {
        $("#agreementModal").fadeOut(200);
        $("#agreeCheck").prop("checked", true).trigger("change");
    });

    $(window).on("click", function (event) {
        if ($(event.target).is("#agreementModal")) {
            $("#agreementModal").fadeOut(200);
        }
    });

    if (!getToken()) {
        redirectToLogin();
        return;
    }

    setReviseModeUi();
    verifyContributorAccess().always(function () {
        if (!shouldContinueLoadingAfterAccessCheck()) {
            finalizeStartup();
            return;
        }

        $.when(loadCategories(), loadTags()).always(function () {
            if (isReviseMode && canSubmitAsContributor) {
                if (!reviseResourceId) {
                    lockSubmissionForm("Missing resource id for revision.");
                    finalizeStartup();
                    return;
                }
                loadRejectedSubmissionForEdit().always(finalizeStartup);
                return;
            }

            loadDraft();
            finalizeStartup();
        });
    });
});
