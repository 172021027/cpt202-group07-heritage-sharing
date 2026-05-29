$(function () {
    var MAX_NOTE_LENGTH = 500;
    var ITEMS_PER_PAGE = 10;
    var currentHistoryPage = 0;
    var latestHistoryResponse = null;

    function showContent() {
        $("#pageLoading").hide();
        $("#pageContent").show();
    }

    function confirmAdminAction(options) {
        if (window.AdminCommon && typeof window.AdminCommon.confirmAction === 'function') {
            return window.AdminCommon.confirmAction(options);
        }

        var deferred = $.Deferred();
        if (window.confirm((options && options.message) || 'Are you sure you want to continue?')) {
            deferred.resolve();
        } else {
            deferred.reject();
        }
        return deferred.promise();
    }

    if (window.AdminCommon && typeof window.AdminCommon.ensureAdminOrRedirect === 'function') {
        window.AdminCommon.ensureAdminOrRedirect();
    }

    var items = [];

    function normalizeStatus(status) {
        var normalized = String(status || '').trim().toUpperCase();
        if (normalized === 'OFFLINE') return 'UNPUBLISHED';
        return normalized;
    }

    function badgeClass(status) {
        var normalized = normalizeStatus(status);
        if (normalized === 'APPROVED') return 'badge approved';
        if (normalized === 'ARCHIVED') return 'badge archived';
        if (normalized === 'UNPUBLISHED') return 'badge unpublished';
        return 'badge';
    }

    function actionsHtml(item) {
        var normalized = normalizeStatus(item.status);
        if (normalized === 'APPROVED') {
            return ''
                + '<button class="admin-btn js-archive" type="button">Archive</button>'
                + '<button class="admin-btn js-unpublish" type="button">Unpublish</button>';
        }

        if (normalized === 'ARCHIVED') {
            return ''
                + '<button class="admin-btn js-unpublish" type="button">Unpublish</button>'
                + '<button class="admin-btn primary js-restore" type="button">Restore</button>';
        }

        if (normalized === 'UNPUBLISHED') {
            return '<button class="admin-btn primary js-restore" type="button">Restore</button>';
        }

        return '';
    }

    function getFiltered() {
        var q = $('#resources-search').val().trim().toLowerCase();
        var filter = normalizeStatus($('#resources-filter').val());

        return items.filter(function (x) {
            if (filter && normalizeStatus(x.status) !== filter) return false;
            if (!q) return true;
            return String(x.title).toLowerCase().indexOf(q) !== -1
                || String(x.category).toLowerCase().indexOf(q) !== -1
                || String(x.status).toLowerCase().indexOf(q) !== -1;
        });
    }

    function render() {
        var list = getFiltered();
        var $tbody = $('#resources-tbody');
        $tbody.empty();

        if (!list.length) {
            $tbody.append('<tr><td colspan="4" class="muted">No resources.</td></tr>');
            return;
        }

        list.forEach(function (x) {
            var html = ''
                + '<tr data-id="' + x.id + '">'
                + '<td>' + x.title + '</td>'
                + '<td class="muted">' + x.category + '</td>'
                + '<td><span class="' + badgeClass(x.status) + '">' + x.status + '</span></td>'
                + '<td><div class="admin-actions">' + actionsHtml(x) + '</div></td>'
                + '</tr>';
            $tbody.append(html);
        });
    }

    function getById(id) {
        return items.find(function (x) { return x.id === id; });
    }

    function parseSortableTime(value) {
        if (!value) return 0;
        var time = new Date(value).getTime();
        return Number.isFinite(time) ? time : 0;
    }

    function loadResources() {
        $.ajax({
            url: '/api/resources',
            type: 'GET',
            success: function (data) {
                items = data.map(function (resource) {
                    var categoryName = String(resource.categoryName || '').trim();
                    return {
                        id: Number(resource.resourceId),
                        title: resource.title || 'Untitled',
                        category: categoryName || ('Category ' + (resource.categoryId == null ? '-' : resource.categoryId)),
                        status: normalizeStatus(resource.status),
                        sortTime: parseSortableTime(
                            resource.submittedAt
                            || resource.submissionDate
                            || resource.createdAt
                            || resource.createTime
                        )
                    };
                });
                items.sort(function (a, b) {
                    if (b.sortTime !== a.sortTime) {
                        return b.sortTime - a.sortTime;
                    }
                    return b.id - a.id;
                });
                render();
                showContent();
            },
            error: function (xhr, status, error) {
                console.error('Error loading resources:', error);
                showContent();
                alert('Failed to load resources. Please try again.');
            }
        });
    }

    $('#resources-search').on('input', render);
    $('#resources-filter').change(render);

    $(document).on('click', '.js-archive', function () {
        var id = Number($(this).closest('tr').data('id'));
        var item = getById(id);
        if (!item) return;
        if (normalizeStatus(item.status) !== 'APPROVED') return;

        confirmAdminAction({
            title: 'Archive resource?',
            message: 'Resource "' + item.title + '" will be archived and removed from the approved list until it is restored.',
            confirmText: 'Archive',
            cancelText: 'Cancel',
            confirmKind: 'danger'
        }).done(function () {
            $.ajax({
                url: '/api/resources/archive/' + id,
                type: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify({ note: '' }),
                success: function (response) {
                    if (response.success) {
                        loadResources();
                        alert('Resource archived successfully.');
                    } else {
                        alert('Failed to archive resource: ' + response.message);
                    }
                },
                error: function (xhr, status, error) {
                    console.error('Error archiving resource:', error);
                    alert('Failed to archive resource. Please try again.');
                }
            });
        });
    });

    var currentUnpublishId = null;

    $(document).on('click', '.js-unpublish', function () {
        var id = Number($(this).closest('tr').data('id'));
        var item = getById(id);
        if (!item) return;
        var normalized = normalizeStatus(item.status);
        if (normalized !== 'APPROVED' && normalized !== 'ARCHIVED') return;

        currentUnpublishId = id;
        $('#unpublish-reason').val('');
        $('#unpublish-modal').css('display', 'block');
    });

    $('.close, #cancel-unpublish').click(function () {
        $('#unpublish-modal').css('display', 'none');
        currentUnpublishId = null;
    });

    $(window).click(function (event) {
        if (event.target.id === 'unpublish-modal') {
            $('#unpublish-modal').css('display', 'none');
            currentUnpublishId = null;
        }
    });

    $('#confirm-unpublish').click(function () {
        if (!currentUnpublishId) return;

        var reason = $('#unpublish-reason').val().trim();
        if (!reason) {
            alert('Please enter a reason for unpublishing.');
            return;
        }
        if (reason.length > MAX_NOTE_LENGTH) {
            alert('Reason must be 500 characters or fewer.');
            return;
        }

        $.ajax({
            url: '/api/resources/offline/' + currentUnpublishId,
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ note: reason }),
            success: function (response) {
                if (response.success) {
                    loadResources();
                    alert('Resource unpublished successfully.\nReason: ' + reason);
                } else {
                    alert('Failed to unpublish resource: ' + response.message);
                }
            },
            error: function (xhr, status, error) {
                console.error('Error unpublishing resource:', error);
                alert('Failed to unpublish resource. Please try again.');
            },
            complete: function () {
                $('#unpublish-modal').css('display', 'none');
                currentUnpublishId = null;
            }
        });
    });

    $(document).on('click', '.js-restore', function () {
        var id = Number($(this).closest('tr').data('id'));
        var item = getById(id);
        if (!item) return;
        var normalized = normalizeStatus(item.status);
        if (normalized !== 'ARCHIVED' && normalized !== 'UNPUBLISHED') return;

        confirmAdminAction({
            title: 'Restore resource?',
            message: 'Resource "' + item.title + '" will return from ' + normalized.toLowerCase() + ' status to the active approved list.',
            confirmText: 'Restore',
            cancelText: 'Cancel',
            confirmKind: 'primary'
        }).done(function () {
            $.ajax({
                url: '/api/resources/restore/' + id,
                type: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify({ note: '' }),
                success: function (response) {
                    if (response.success) {
                        loadResources();
                        alert('Resource restored successfully.');
                    } else {
                        alert('Failed to restore resource: ' + response.message);
                    }
                },
                error: function (xhr, status, error) {
                    console.error('Error restoring resource:', error);
                    alert('Failed to restore resource. Please try again.');
                }
            });
        });
    });

    /**
     * Retrieve pagination operation history from backend API
     */
    function loadHistoryData(page) {
        $.ajax({
            url: '/api/resources/actions/history',
            type: 'GET',
            data: {
                page: page,
                size: ITEMS_PER_PAGE
            },
            success: function (response) {
                if (response.success) {
                    populateHistoryActionFilter(response.actionTypes);
                    renderHistory(response);
                } else {
                    alert('Failed to load history: ' + response.message);
                }
            },
            error: function (xhr, status, error) {
                console.error('Error loading history:', error);
                alert('Failed to load history. Please try again.');
            }
        });
    }

    function populateHistoryActionFilter(actionTypes) {
        var $filter = $('#history-action-filter');
        var current = String($filter.val() || '').trim().toUpperCase();
        var options = Array.isArray(actionTypes) ? actionTypes : [];
        options = options
            .map(function (type) { return String(type || '').trim().toUpperCase(); })
            .filter(function (type) { return type.length > 0; });
        options = Array.from(new Set(options));

        var html = '<option value="">All Action Types</option>';
        options.forEach(function (type) {
            html += '<option value="' + type + '">' + type + '</option>';
        });
        $filter.html(html);

        if (current && options.indexOf(current) !== -1) {
            $filter.val(current);
        } else {
            $filter.val('');
        }
    }

    /**
     * Render the history table
     */
    function renderHistory(response) {
        latestHistoryResponse = response;
        var $tbody = $('#history-tbody');
        $tbody.empty();

        var selectedActionType = String($('#history-action-filter').val() || '').trim().toUpperCase();
        var data = Array.isArray(response.data) ? response.data : [];
        if (selectedActionType) {
            data = data.filter(function (record) {
                return String(record.actionType || '').trim().toUpperCase() === selectedActionType;
            });
        }

        if (!data || data.length === 0) {
            $tbody.append('<tr><td colspan="5" class="muted">No history records for selected action type on this page.</td></tr>');
            renderPagination(response);
            return;
        }

        data.forEach(function (record) {
            var html = ''
                + '<tr>'
                + '<td>' + record.actionAt + '</td>'
                + '<td>' + record.resourceTitle + '</td>'
                + '<td>' + record.actionType + '</td>'
                + '<td>' + record.actionByUserName + '</td>'
                + '<td>' + (record.feedbackText || '-') + '</td>'
                + '</tr>';
            $tbody.append(html);
        });

        renderPagination(response);
    }

    /**
     * Render pagination controls
     */
    function renderPagination(response) {
        var $pagination = $('#history-pagination');
        $pagination.empty();

        var currentPage = response.currentPage;
        var totalPages = response.totalPages;

        if (totalPages <= 1) {
            return;
        }

        // 上一页按钮
        if (response.hasPrevious) {
            var $prevBtn = $('<button type="button" class="pagination-btn">Previous</button>');
            $prevBtn.click(function () {
                currentHistoryPage = currentPage - 1;
                loadHistoryData(currentHistoryPage);
            });
            $pagination.append($prevBtn);
        } else {
            var $disabledPrevBtn = $('<button type="button" class="pagination-btn" disabled>Previous</button>');
            $pagination.append($disabledPrevBtn);
        }

        // 页码显示
        var pageInfoHtml = '<span style="padding: 0.5rem 1rem;">Page ' + (currentPage + 1) + ' of ' + totalPages + '</span>';
        $pagination.append(pageInfoHtml);

        // 下一页按钮
        if (response.hasNext) {
            var $nextBtn = $('<button type="button" class="pagination-btn">Next</button>');
            $nextBtn.click(function () {
                currentHistoryPage = currentPage + 1;
                loadHistoryData(currentHistoryPage);
            });
            $pagination.append($nextBtn);
        } else {
            var $disabledNextBtn = $('<button type="button" class="pagination-btn" disabled>Next</button>');
            $pagination.append($disabledNextBtn);
        }
    }

    $('#resources-history').click(function () {
        // Reset pagination state and load the first page
        currentHistoryPage = 0;
        loadHistoryData(currentHistoryPage);
        $('#history-modal').css('display', 'block');
    });

    $('#history-action-filter').on('change', function () {
        if (latestHistoryResponse) {
            renderHistory(latestHistoryResponse);
        }
    });

    $('#close-history, #history-modal .close').click(function () {
        $('#history-modal').css('display', 'none');
    });

    $(window).click(function (event) {
        if (event.target.id === 'history-modal') {
            $('#history-modal').css('display', 'none');
        }
    });

    loadResources();
});
