$(function () {
    var MAX_NOTE_LENGTH = 500;
    var query = getQuery();
    var resourceId = Number(query.resourceId || 0);
    var resource = null;
    var isSubmitting = false;

    function showContent() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    function startPage() {
        if (!resourceId) {
            if (window.AdminCommon && typeof window.AdminCommon.pushFlash === 'function') {
                window.AdminCommon.pushFlash('error', 'Invalid resourceId.');
            } else {
                alert('Invalid resourceId.');
            }
            gotoList();
            return;
        }

        loadReviewDetail(resourceId);
    }

    if (window.AdminCommon && typeof window.AdminCommon.ensureAdminOrRedirect === 'function') {
        window.AdminCommon.ensureAdminOrRedirect(startPage);
    } else {
        startPage();
    }

    function getQuery() {
        var out = {};
        var q = window.location.search.replace(/^\?/, '');
        if (!q) return out;
        q.split('&').forEach(function (pair) {
            var idx = pair.indexOf('=');
            if (idx === -1) return;
            var k = decodeURIComponent(pair.slice(0, idx));
            out[k] = decodeURIComponent(pair.slice(idx + 1));
        });
        return out;
    }

    function formatTags(tags) {
        if (!Array.isArray(tags) || !tags.length) return '-';
        return tags
            .map(function (t) { return String((t && t.tagName) || '').trim(); })
            .filter(function (name) { return !!name; })
            .join(', ') || '-';
    }

    function escapeHtml(text) {
        return String(text || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function normalizeAssetPath(value) {
        return typeof value === 'string' ? value.replace(/\\/g, '/') : '';
    }

    function isAssetPath(value) {
        var normalized = normalizeAssetPath(value);
        return normalized.length > 0
            && (normalized.startsWith('/')
                || normalized.startsWith('uploads/')
                || normalized.startsWith('http://')
                || normalized.startsWith('https://')
                || normalized.startsWith('data:'));
    }

    function toAssetUrl(value) {
        var normalized = normalizeAssetPath(value);
        if (!isAssetPath(normalized)) {
            return '';
        }
        if (normalized.startsWith('http://') || normalized.startsWith('https://') || normalized.startsWith('data:')) {
            return normalized;
        }
        return normalized.startsWith('/') ? normalized : '/' + normalized;
    }

    function buildResourceContentHtml(resource) {
        if (!resource) return '<div class="muted">-</div>';

        var contributorLabel = String(resource.contributorUsername || '').trim();
        if (!contributorLabel) {
            contributorLabel = resource.contributorId == null ? '-' : String(resource.contributorId);
        } else if (resource.contributorId != null) {
            contributorLabel += ' (#' + resource.contributorId + ')';
        }
        var textContent = escapeHtml(resource.description || '-');
        var pictureUrl = toAssetUrl(resource.picturePath);
        var videoUrl = toAssetUrl(resource.videoPath);
        var copyrightText = escapeHtml(resource.copyrightDeclaration || '-');
        var statusClass = resource.status === 'pending_review' ? 'badge pending' : 'badge';
        var pictureHtml = pictureUrl
            ? '<img class="rd-media-image" src="' + escapeHtml(pictureUrl) + '" alt="Submission picture preview">'
            : '<div class="muted">No picture provided.</div>';
        var videoHtml = videoUrl
            ? '<video class="rd-media-video" src="' + escapeHtml(videoUrl) + '" controls preload="metadata"></video>'
            : '<div class="muted">No video provided.</div>';

        return ''
            + '<div class="rd-submission-content">'
            + '<div><strong>Resource ID</strong>: ' + escapeHtml(resource.resourceId) + '</div>'
            + '<div><strong>Title</strong>: ' + escapeHtml(resource.title) + '</div>'
            + '<div><strong>Category</strong>: ' + escapeHtml(resource.category) + '</div>'
            + '<div><strong>Contributor</strong>: ' + escapeHtml(contributorLabel) + '</div>'
            + '<div><strong>Tags</strong>: ' + escapeHtml(formatTags(resource.tags)) + '</div>'
            + '<div><strong>Location</strong>: ' + escapeHtml(resource.location || '-') + '</div>'
            + '<div><strong>Submitted At</strong>: ' + escapeHtml(resource.submittedAt || '-') + '</div>'
            + '<div><strong>Status</strong>: <span class="' + statusClass + '">' + escapeHtml(resource.status || 'pending_review') + '</span></div>'
            + '<div><strong>Description</strong></div>'
            + '<div class="rd-text-content">' + textContent + '</div>'
            + '<div><strong>Copyright declaration</strong></div>'
            + '<div class="rd-text-content">' + copyrightText + '</div>'
            + '<div class="rd-media-grid">'
            + '<div class="rd-media-card"><div><strong>Picture</strong></div>' + pictureHtml + '</div>'
            + '<div class="rd-media-card"><div><strong>Video</strong></div>' + videoHtml + '</div>'
            + '</div>'
            + '</div>';
    }

    function gotoList() {
        window.location.href = 'review-list.html';
    }

    function showPageError(message) {
        if (window.AdminCommon && typeof window.AdminCommon.showFlashMessage === 'function') {
            window.AdminCommon.showFlashMessage('error', message);
            return;
        }
        alert(message);
    }

    function setSubmitting(nextSubmitting) {
        isSubmitting = !!nextSubmitting;
        $('#rd-note').prop('disabled', isSubmitting);
        $('#rd-approve, #rd-reject').prop('disabled', isSubmitting);
    }

    function setFeedbackError(message) {
        var $input = $('#rd-note');
        var $err = $('#rd-note-error');

        if (message) {
            $input.addClass('invalid');
            $err.text(message).show();
            return false;
        }

        $input.removeClass('invalid');
        $err.text('').hide();
        return true;
    }

    function validateFeedback() {
        var note = $('#rd-note').val();
        note = String(note || '').trim();
        if (!note) return setFeedbackError('Feedback is required.');
        if (note.length > MAX_NOTE_LENGTH) return setFeedbackError('Feedback must be 500 characters or fewer.');
        return setFeedbackError('');
    }

    function submitDecision(decision) {
        if (!resource) {
            showPageError('No resource selected.');
            return;
        }

        if (isSubmitting) return;

        if (decision !== 'approve' && decision !== 'reject') {
            showPageError('Invalid review decision.');
            return;
        }

        if (!validateFeedback()) return;

        var note = $('#rd-note').val().trim();
        setSubmitting(true);

        $.ajax({
            url: '/api/admin/reviews',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                resourceId: resource.resourceId,
                submissionId: 'resource-' + resource.resourceId,
                decision: decision,
                note: note
            })
        }).done(function (response) {
            if (window.AdminCommon && typeof window.AdminCommon.pushFlash === 'function') {
                window.AdminCommon.pushFlash('success', (response && response.message) || 'Review decision recorded successfully.');
            }
            gotoList();
        }).fail(function (xhr) {
            var msg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : 'Failed to submit review decision.';
            showPageError(msg);
        }).always(function () {
            setSubmitting(false);
        });
    }

    $('#rd-approve').click(function () { submitDecision('approve'); });
    $('#rd-reject').click(function () { submitDecision('reject'); });

    $('#rd-note').on('input', function () {
        // live-validate to clear error as user types
        validateFeedback();
    });

    function loadReviewDetail(id) {
        if (window.AdminCommon && typeof window.AdminCommon.clearFlashMessage === 'function') {
            window.AdminCommon.clearFlashMessage();
        }
        $.getJSON('/api/admin/reviews/' + encodeURIComponent(id))
            .done(function (data) {
                resource = data || null;
                if (!resource) {
                    showContent();
                    if (window.AdminCommon && typeof window.AdminCommon.pushFlash === 'function') {
                        window.AdminCommon.pushFlash('error', 'Resource not found.');
                    } else {
                        alert('Resource not found.');
                    }
                    gotoList();
                    return;
                }
                $('#rd-content-current').html(buildResourceContentHtml(resource));
                showContent();
            })
            .fail(function (xhr) {
                showContent();
                var msg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
                    ? xhr.responseJSON.message
                    : 'Failed to load review detail.';
                if (window.AdminCommon && typeof window.AdminCommon.pushFlash === 'function') {
                    window.AdminCommon.pushFlash('error', msg);
                } else {
                    alert(msg);
                }
                gotoList();
            });
    }

});
