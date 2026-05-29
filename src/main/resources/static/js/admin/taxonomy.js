var categoriesLoaded = false;
var tagsLoaded = false;

function loadCategories() {
    $.ajax({
        url: '/api/categories?_=' + Date.now(),
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        success: function (categories) {
            renderCategories(categories);
            clearCategoryError();
            categoriesLoaded = true;
            checkAllLoaded();
        },
        error: function (xhr) {
            showCategoryError('Failed to load categories: ' + xhr.status);
            categoriesLoaded = true;
            checkAllLoaded();
        }
    });
}

function loadTags() {
    $.ajax({
        url: '/api/tags?_=' + Date.now(),
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        success: function (tags) {
            renderTags(tags);
            populateTagDropdowns(tags);
            clearTagError();
            tagsLoaded = true;
            checkAllLoaded();
        },
        error: function (xhr) {
            showTagError('Failed to load tags: ' + xhr.status);
            tagsLoaded = true;
            checkAllLoaded();
        }
    });
}

function showContent() {
    $("#pageLoading").hide();
    $("#pageContent").show();
}

function checkAllLoaded() {
    if (categoriesLoaded && tagsLoaded) {
        showContent();
    }
}

function confirmAdminDanger(options) {
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

function setupEventListeners() {
    $('#add-category').click(handleAddCategory);
    $('#add-tag').click(handleAddTag);
    $('#merge-tags').click(handleMergeTags);
    $('#taxonomy-search').on('input', handleSearch);
}

function startPage() {
    categoriesLoaded = false;
    tagsLoaded = false;
    loadCategories();
    loadTags();
    setupEventListeners();
}

$(document).ready(function () {
    if (window.AdminCommon && typeof window.AdminCommon.ensureAdminOrRedirect === 'function') {
        window.AdminCommon.ensureAdminOrRedirect(startPage);
    } else {
        startPage();
    }
});

// ===== Categories Management =====

function renderCategories(categories) {
    var tbody = $('#categories-tbody');
    tbody.empty();

    if (!categories || categories.length === 0) {
        tbody.html('<tr><td colspan="2" style="text-align: center; color: #999;">No categories found</td></tr>');
        return;
    }

    categories.forEach(function (category) {
        var html = '<tr>'
            + '<td>' + escapeHtml(category.categoryName) + '</td>'
            + '<td>'
            + '<button class="admin-btn small edit-category-btn" data-id="' + category.categoryId + '">Edit</button> '
            + '<button class="admin-btn small danger delete-category-btn" data-id="' + category.categoryId + '">Delete</button>'
            + '</td>'
            + '</tr>';

        var $row = $(html);
        $row.find('.edit-category-btn').click(function () {
            handleEditCategory(category.categoryId, category.categoryName);
        });
        $row.find('.delete-category-btn').click(function () {
            handleDeleteCategory(category.categoryId, category.categoryName);
        });
        tbody.append($row);
    });
}

function handleAddCategory() {
    var name = window.prompt('Enter category name:');
    if (name && name.trim()) {
        var categoryName = name.trim();

        $.ajax({
            url: '/api/categories',
            method: 'POST',
            contentType: 'application/json',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            data: JSON.stringify({
                categoryName: categoryName
            }),
            success: function () {
                loadCategories();
                clearCategoryError();
            },
            error: function () {
                showCategoryError('Failed to add category');
            }
        });
    }
}

function handleEditCategory(categoryId, currentName) {
    var newName = window.prompt('Edit category name:', currentName);
    if (newName && newName.trim() && newName !== currentName) {
        var categoryName = newName.trim();

        $.ajax({
            url: '/api/categories/' + categoryId,
            method: 'PUT',
            contentType: 'application/json',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            data: JSON.stringify({
                categoryName: categoryName
            }),
            success: function () {
                loadCategories();
                clearCategoryError();
            },
            error: function () {
                showCategoryError('Failed to update category');
            }
        });
    }
}

function handleDeleteCategory(categoryId, categoryName) {
    confirmAdminDanger({
        title: 'Delete category?',
        message: 'Category "' + String(categoryName || 'Unnamed') + '" will be removed permanently.',
        confirmText: 'Delete',
        cancelText: 'Cancel',
        confirmKind: 'danger'
    }).done(function () {
        $.ajax({
            url: '/api/categories/' + categoryId,
            method: 'DELETE',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            success: function () {
                loadCategories();
                clearCategoryError();
            },
            error: function () {
                showCategoryError('Failed to delete category');
            }
        });
    });
}

function showCategoryError(message) {
    $('#category-error').text(message).show();
}

function clearCategoryError() {
    $('#category-error').text('').hide();
}

// ===== Tags Management =====

function renderTags(tags) {
    var tbody = $('#tags-tbody');
    tbody.empty();

    if (!tags || tags.length === 0) {
        tbody.html('<tr><td colspan="2" style="text-align: center; color: #999;">No tags found</td></tr>');
        return;
    }

    tags.forEach(function (tag) {
        var html = '<tr>'
            + '<td>' + escapeHtml(tag.tagName) + '</td>'
            + '<td>'
            + '<button class="admin-btn small edit-tag-btn" data-id="' + tag.tagId + '">Edit</button> '
            + '<button class="admin-btn small danger delete-tag-btn" data-id="' + tag.tagId + '">Delete</button>'
            + '</td>'
            + '</tr>';

        var $row = $(html);
        $row.find('.edit-tag-btn').click(function () {
            handleEditTag(tag.tagId, tag.tagName);
        });
        $row.find('.delete-tag-btn').click(function () {
            handleDeleteTag(tag.tagId, tag.tagName);
        });
        tbody.append($row);
    });
}

function handleAddTag() {
    var name = window.prompt('Enter tag name:');
    if (name && name.trim()) {
        var tagName = name.trim();

        $.ajax({
            url: '/api/tags',
            method: 'POST',
            contentType: 'application/json',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            data: JSON.stringify({
                tagName: tagName
            }),
            success: function () {
                loadTags();
                clearTagError();
            },
            error: function () {
                showTagError('Failed to add tag');
            }
        });
    }
}

function handleEditTag(tagId, currentName) {
    var newName = window.prompt('Edit tag name:', currentName);
    if (newName && newName.trim() && newName !== currentName) {
        var tagName = newName.trim();

        $.ajax({
            url: '/api/tags/' + tagId,
            method: 'PUT',
            contentType: 'application/json',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            data: JSON.stringify({
                tagName: tagName
            }),
            success: function () {
                loadTags();
                clearTagError();
            },
            error: function () {
                showTagError('Failed to update tag');
            }
        });
    }
}

function handleDeleteTag(tagId, tagName) {
    confirmAdminDanger({
        title: 'Delete tag?',
        message: 'Tag "' + String(tagName || 'Unnamed') + '" will be removed permanently.',
        confirmText: 'Delete',
        cancelText: 'Cancel',
        confirmKind: 'danger'
    }).done(function () {
        $.ajax({
            url: '/api/tags/' + tagId,
            method: 'DELETE',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            success: function () {
                loadTags();
                clearTagError();
            },
            error: function () {
                showTagError('Failed to delete tag');
            }
        });
    });
}

function showTagError(message) {
    $('#tag-error').text(message).show();
}

function clearTagError() {
    $('#tag-error').text('').hide();
}

// ===== Merge Tags =====

function populateTagDropdowns(tags) {
    var secondarySelect = $('#secondary-tag');
    var primarySelect = $('#primary-tag');

    var secondaryValue = secondarySelect.val();
    var primaryValue = primarySelect.val();

    secondarySelect.empty().append('<option value="">Select Secondary Tag</option>');
    primarySelect.empty().append('<option value="">Select Primary Tag</option>');

    if (tags) {
        tags.forEach(function (tag) {
            secondarySelect.append('<option value="' + tag.tagId + '">' + escapeHtml(tag.tagName) + '</option>');
            primarySelect.append('<option value="' + tag.tagId + '">' + escapeHtml(tag.tagName) + '</option>');
        });
    }

    if (secondaryValue) secondarySelect.val(secondaryValue);
    if (primaryValue) primarySelect.val(primaryValue);
}

function handleMergeTags() {
    var secondaryTagId = $('#secondary-tag').val();
    var primaryTagId = $('#primary-tag').val();

    if (!secondaryTagId || !primaryTagId) {
        showMergeError('Please select both secondary and primary tags');
        return;
    }

    if (secondaryTagId === primaryTagId) {
        showMergeError('Secondary and primary tags must be different');
        return;
    }

    var secondaryTagName = $('#secondary-tag option:selected').text();
    var primaryTagName = $('#primary-tag option:selected').text();

    confirmAdminDanger({
        title: 'Merge tags?',
        message: 'Secondary tag "' + secondaryTagName + '" will be merged into "' + primaryTagName + '".\n\nResources using the secondary tag will be reassigned, then the secondary tag will be deleted.',
        confirmText: 'Merge tags',
        cancelText: 'Cancel',
        confirmKind: 'danger'
    }).done(function () {
        $.ajax({
            url: '/api/tags/merge?secondaryTagId=' + encodeURIComponent(secondaryTagId)
                + '&primaryTagId=' + encodeURIComponent(primaryTagId),
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            success: function () {
                $('#secondary-tag').val('');
                $('#primary-tag').val('');
                clearMergeError();
                loadTags();
                alert('Tags merged successfully');
            },
            error: function (xhr) {
                var errorMsg = 'Failed to merge tags';
                if (xhr.responseJSON && xhr.responseJSON.error) {
                    errorMsg = xhr.responseJSON.error;
                }
                showMergeError(errorMsg);
            }
        });
    });
}

function showMergeError(message) {
    $('#merge-error').text(message).show();
}

function clearMergeError() {
    $('#merge-error').text('').hide();
}

// ===== Search =====

function handleSearch() {
    var searchTerm = $('#taxonomy-search').val().toLowerCase();

    $.ajax({
        url: '/api/categories',
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        success: function (categories) {
            var filtered = categories.filter(function (c) {
                return c.categoryName.toLowerCase().indexOf(searchTerm) !== -1;
            });
            renderCategories(filtered);
        }
    });

    $.ajax({
        url: '/api/tags',
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('token')
        },
        success: function (tags) {
            var filtered = tags.filter(function (t) {
                return t.tagName.toLowerCase().indexOf(searchTerm) !== -1;
            });
            renderTags(filtered);
            populateTagDropdowns(tags);
        }
    });
}

// ===== Utility Functions =====

function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
