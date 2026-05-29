// 简单的XOR加密函数
function encryptPassword(password) {
    let key = 'heritage_sharing_platform';
    let encrypted = '';
    for (let i = 0; i < password.length; i++) {
        encrypted += String.fromCharCode(password.charCodeAt(i) ^ key.charCodeAt(i % key.length));
    }
    return btoa(encrypted);
}

$(document).ready(function () {
    function saveLoginState(response) {
        if (window.AuthState) {
            window.AuthState.saveAuth(response.token, response.user);
            return window.AuthState.normalizeUser(response.user);
        }

        localStorage.setItem('token', response.token);
        localStorage.setItem('user', JSON.stringify(response.user));
        return response.user;
    }

    function isAdminRole(role) {
        return window.AuthState
            ? window.AuthState.isAdminRole(role)
            : String(role || '').trim().toLowerCase() === 'admin';
    }

    function redirectAfterLogin(user) {
        if (window.AuthState) {
            window.AuthState.redirectAfterLogin(user);
            return;
        }
        window.location.href = isAdminRole(user && user.role) ? 'admin.html' : 'search.html';
    }

    function showLoginTabs() {
        $('#main-form-header h2').text('Welcome Back');
        $('.form-tabs').show();
        $('.tab-content').hide();
        $('#register, #forgot-password-form-container').hide();
    }

    function showRegisterForm() {
        $('#main-form-header h2').text('Create Account');
        $('.form-tabs').hide();
        $('.tab-content').hide();
        $('#email-password, #email-code, #admin-tab, #forgot-password-form-container').hide();
        $('#register').show();
        $('#register-username, #register-email, #register-code, #register-password, #confirm-password').val('');

        // Clear all code timers
        for (const buttonId in codeTimers) {
            if (codeTimers[buttonId]) {
                clearInterval(codeTimers[buttonId]);
                codeTimers[buttonId] = null;
            }
        }

        // Reset send code buttons
        $('#send-code, #send-register-code, #send-reset-code').each(function () {
            $(this).text('Send Code');
            $(this).prop('disabled', false);
        });
    }

    function showResetPasswordForm() {
        $('#main-form-header h2').text('Reset Password');
        $('.form-tabs').hide();
        $('.tab-content').hide();
        $('#email-password, #email-code, #admin-tab, #register').hide();
        $('#forgot-password-form-container').show();
        $('#reset-email, #reset-code, #new-password, #confirm-new-password').val('');

        // Clear all code timers
        for (const buttonId in codeTimers) {
            if (codeTimers[buttonId]) {
                clearInterval(codeTimers[buttonId]);
                codeTimers[buttonId] = null;
            }
        }

        // Reset send code buttons
        $('#send-code, #send-register-code, #send-reset-code').each(function () {
            $(this).text('Send Code');
            $(this).prop('disabled', false);
        });
    }

    function showEmailPasswordTab() {
        showLoginTabs();
        $('.tab-btn').removeClass('active');
        $('.tab-content').removeClass('active');
        $('[data-tab="email-password"]').addClass('active');
        $('#email-password').addClass('active').show();

        // Clear all code timers
        for (const buttonId in codeTimers) {
            if (codeTimers[buttonId]) {
                clearInterval(codeTimers[buttonId]);
                codeTimers[buttonId] = null;
            }
        }

        // Reset send code buttons
        $('#send-code, #send-register-code, #send-reset-code').each(function () {
            $(this).text('Send Code');
            $(this).prop('disabled', false);
        });
    }

    $('.tab-btn').click(function () {
        const tabId = $(this).data('tab');
        $('.tab-btn').removeClass('active');
        $(this).addClass('active');
        $('.tab-content').removeClass('active').hide();

        // Clear all input fields when switching tabs
        $('#email, #password, #code-email, #verification-code, #admin-username, #admin-password').val('');

        // Clear all code timers
        for (const buttonId in codeTimers) {
            if (codeTimers[buttonId]) {
                clearInterval(codeTimers[buttonId]);
                codeTimers[buttonId] = null;
            }
        }

        // Reset all send code buttons to their original state
        $('#send-code, #send-register-code, #send-reset-code').each(function () {
            $(this).text('Send Code');
            $(this).prop('disabled', false);
        });

        if (tabId === 'admin') {
            $('#admin-tab').addClass('active').show();
        } else {
            $('#' + tabId).addClass('active').show();
        }
    });

    $('#show-register, #show-register-code').click(function (e) {
        e.preventDefault();
        showRegisterForm();
    });

    $('#cancel-register').click(function (e) {
        e.preventDefault();
        showEmailPasswordTab();
    });

    $('#back-to-login').click(function (e) {
        e.preventDefault();
        showEmailPasswordTab();
    });

    $('.password-toggle').click(function () {
        const button = $(this);
        const targetId = button.data('target');
        const passwordInput = $('#' + targetId);
        const eyeIcon = button.find('.eye-icon');
        const eyeOffIcon = button.find('.eye-off-icon');

        if (passwordInput.attr('type') === 'password') {
            passwordInput.attr('type', 'text');
            eyeIcon.addClass('hidden');
            eyeOffIcon.removeClass('hidden');
        } else {
            passwordInput.attr('type', 'password');
            eyeIcon.removeClass('hidden');
            eyeOffIcon.addClass('hidden');
        }
    });

    $('#forgot-password').click(function (e) {
        e.preventDefault();
        showResetPasswordForm();
    });

    // Cancel reset password
    $('#cancel-reset').click(function (e) {
        e.preventDefault();
        showEmailPasswordTab();
    });

    // Back to login from reset password
    $('#back-to-login-from-reset').click(function (e) {
        e.preventDefault();
        showEmailPasswordTab();
    });

    // Store timer references
    let codeTimers = {};

    // Send verification code functionality
    $('#send-code, #send-register-code, #send-reset-code').click(function () {
        const button = $(this);
        const buttonId = button.attr('id');
        const originalText = button.text();

        // Clear any existing timer for this button
        if (codeTimers[buttonId]) {
            clearInterval(codeTimers[buttonId]);
            codeTimers[buttonId] = null;
        }

        // Get the corresponding email input based on the button ID
        let emailInput;
        if (button.attr('id') === 'send-code') {
            emailInput = $('#code-email'); // Email & Code tab
        } else if (button.attr('id') === 'send-register-code') {
            emailInput = $('#register-email'); // Register tab
        } else if (button.attr('id') === 'send-reset-code') {
            emailInput = $('#reset-email'); // Reset password tab
        }

        // Validate email
        const email = emailInput.val().trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (!email || !emailRegex.test(email)) {
            alert('Please enter a valid email address!');
            emailInput.focus();
            return;
        }

        // Show loading state
        button.text('Sending...');
        button.prop('disabled', true);

        // For all cases, check if email exists first
        $.ajax({
            url: '/api/auth/check-email',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (response) {
                // Email exists(Email is not null and email not belongs to an admin)
                if (button.attr('id') === 'send-register-code') {
                    // For registration, email should not exist
                    alert('Email already exists! Please use a different email address.');
                    emailInput.val('');
                    button.text(originalText);
                    button.prop('disabled', false);
                } else {
                    // For login and password reset, email should exist, proceed to send code
                    sendVerificationCode();
                }
            },
            error: function (xhr) {
                // Email does not exist or the admin email
                if (button.attr('id') === 'send-register-code') {
                    if (xhr.responseText === "Email not found") sendVerificationCode();
                    else {
                        alert('Email already exists! Please use a different email address.');
                        emailInput.val('');
                        button.text(originalText);
                        button.prop('disabled', false);
                    }
                } else {
                    // For login and password reset, email should exist
                    alert('Email not found! Please check your email address.');
                    emailInput.val('');
                    button.text(originalText);
                    button.prop('disabled', false);
                }
            }
        });

        function sendVerificationCode() {
            // Call send verification code API
            $.ajax({
                url: '/api/auth/send-verification-code',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ email: email }),
                success: function (response) {
                    // Show verification code
                    alert('Verification code is: ' + response.code);

                    // Start 60-second countdown
                    let countdown = 60;
                    button.text(countdown + 's');

                    codeTimers[buttonId] = setInterval(function () {
                        countdown--;
                        button.text(countdown + 's');

                        if (countdown <= 0) {
                            clearInterval(codeTimers[buttonId]);
                            codeTimers[buttonId] = null;
                            button.text(originalText);
                            button.prop('disabled', false);
                        }
                    }, 1000);
                },
                error: function (xhr) {
                    alert('Failed to send verification code: ' + (xhr.responseText || 'Please try again later'));
                    button.text(originalText);
                    button.prop('disabled', false);
                }
            });
        }
    });

    // Email & Password login form submission
    $('#email-password-form').submit(function (e) {
        e.preventDefault();

        const email = $('#email').val();
        const password = $('#password').val();

        // Show loading state
        const submitButton = $(this).find('button[type="submit"]');
        const originalText = submitButton.text();
        submitButton.text('Signing in...');
        submitButton.prop('disabled', true);

        // Call login API
        $.ajax({
            url: '/api/auth/login',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email, password: encryptPassword(password) }),
            success: function (response) {
                const user = saveLoginState(response);

                // Show success message
                alert('Login successful!');

                // Redirect based on role
                redirectAfterLogin(user);
            },
            error: function (xhr) {
                alert('Login failed: ' + (xhr.responseText || 'Invalid email or password'));
                // Clear input fields on login failure
                $('#email, #password').val('');
            },
            complete: function () {
                submitButton.text(originalText);
                submitButton.prop('disabled', false);
            }
        });
    });

    // Admin login form submission
    $('#admin-form').submit(function (e) {
        e.preventDefault();

        const email = $('#admin-username').val();
        const password = $('#admin-password').val();

        // Show loading state
        const submitButton = $(this).find('button[type="submit"]');
        const originalText = submitButton.text();
        submitButton.text('Signing in...');
        submitButton.prop('disabled', true);

        // Call login API
        $.ajax({
            url: '/api/auth/admin/login',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email, password: encryptPassword(password) }),
            success: function (response) {
                // Verify admin role - only admin role can login via admin tab
                if (isAdminRole(response.user.role)) {
                    const user = saveLoginState(response);

                    // Show success message
                    alert('Login successful!');

                    redirectAfterLogin(user);
                } else {
                    // Non-admin user trying to login via admin tab
                    alert('Login failed: Invalid email or password');
                    $('#admin-username, #admin-password').val('');
                }
            },
            error: function (xhr) {
                alert('Login failed: Invalid email or password');
                // Clear input fields on login failure
                $('#admin-username, #admin-password').val('');
            },
            complete: function () {
                submitButton.text(originalText);
                submitButton.prop('disabled', false);
            }
        });
    });

    // Email & Code login form submission
    $('#email-code-form').submit(function (e) {
        e.preventDefault();

        const email = $('#code-email').val();
        const code = $('#verification-code').val();

        // Validate verification code
        if (!code) {
            alert('Please enter verification code!');
            $('#verification-code').focus();
            return;
        }

        // Show loading state
        const submitButton = $(this).find('button[type="submit"]');
        const originalText = submitButton.text();
        submitButton.text('Signing in...');
        submitButton.prop('disabled', true);

        // Call login API
        $.ajax({
            url: '/api/auth/login',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email, verificationCode: code }),
            success: function (response) {
                const user = saveLoginState(response);

                // Show success message
                alert('Login successful!');

                // Redirect based on role
                redirectAfterLogin(user);
            },
            error: function (xhr) {
                alert('Login failed: ' + (xhr.responseText || 'Invalid email or verification code'));
                // Clear input fields on login failure
                $('#verification-code').val('');
            },
            complete: function () {
                submitButton.text(originalText);
                submitButton.prop('disabled', false);
            }
        });
    });

    // Register form submission
    $('#register-form').submit(function (e) {
        e.preventDefault();

        const username = $('#register-username').val();
        const email = $('#register-email').val();
        const code = $('#register-code').val();
        const password = $('#register-password').val();
        const confirmPassword = $('#confirm-password').val();

        // Validate password match
        if (password !== confirmPassword) {
            alert('Passwords do not match!');
            $('#register-password, #confirm-password').val('');
            return;
        }

        // Show loading state
        const submitButton = $(this).find('button[type="submit"]');
        const originalText = submitButton.text();
        submitButton.text('Signing up...');
        submitButton.prop('disabled', true);

        // First check if email exists
        $.ajax({
            url: '/api/auth/check-email',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (response) {
                // Email exists, registration failed
                alert('Registration failed: Email already exists');
                submitButton.text(originalText);
                submitButton.prop('disabled', false);
            },
            error: function (xhr) {
                // Email does not exist, proceed with password validation
                // Validate password length
                if (password.length < 8) {
                    alert('Password must be at least 8 characters long!');
                    $('#register-password, #confirm-password').val('');
                    submitButton.text(originalText);
                    submitButton.prop('disabled', false);
                    return;
                }

                // Call register API (verification code will be validated on server)
                $.ajax({
                    url: '/api/auth/register',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify({ username, email, password: encryptPassword(password), verificationCode: code }),
                    success: function (response) {
                        // Registration successful, redirect to login page
                        showEmailPasswordTab();
                        alert('Registration successful! Please login with your new account.');
                    },
                    error: function (xhr) {
                        alert('Registration failed: ' + (xhr.responseText || 'Invalid verification code'));
                        // Only clear verification code field on failure
                        if (xhr.responseText && xhr.responseText.includes('Invalid verification code')) {
                            $('#register-code').val('');
                        }
                    },
                    complete: function () {
                        submitButton.text(originalText);
                        submitButton.prop('disabled', false);
                    }
                });
            }
        });
    });

    // Forgot password form submission
    $('#forgot-password-form').submit(function (e) {
        e.preventDefault();

        const email = $('#reset-email').val();
        const code = $('#reset-code').val();
        const newPassword = $('#new-password').val();
        const confirmNewPassword = $('#confirm-new-password').val();

        // Validate verification code
        if (!code) {
            alert('Please enter verification code!');
            $('#reset-code').focus();
            return;
        }

        // Validate password length
        if (newPassword.length < 8) {
            alert('Password must be at least 8 characters long!');
            $('#new-password').val('');
            $('#confirm-new-password').val('');
            return;
        }

        // Validate password match
        if (newPassword !== confirmNewPassword) {
            alert('Passwords do not match!');
            $('#new-password').val('');
            $('#confirm-new-password').val('');
            return;
        }

        // Check if email exists
        $.ajax({
            url: '/api/auth/check-email',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (response) {
                // Email exists, proceed with password reset
                resetPassword();
            },
            error: function (xhr) {
                // Email does not exist
                alert('Email not found! Please check your email address.');
                $('#reset-email, #reset-code, #new-password, #confirm-new-password').val('');
            }
        });

        function resetPassword() {
            const email = $('#reset-email').val();
            const newPassword = $('#new-password').val();
            const code = $('#reset-code').val();

            // Show loading state
            const submitButton = $('#forgot-password-form').find('button[type="submit"]');
            const originalText = submitButton.text();
            submitButton.text('Resetting...');
            submitButton.prop('disabled', true);

            // Call password reset API
            $.ajax({
                url: '/api/auth/reset-password',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ email, newPassword: encryptPassword(newPassword), verificationCode: code }),
                success: function (response) {
                    // Password reset successful, show alert first then redirect to login page
                    alert('Password reset successfully! Please login with your new password.');
                    showEmailPasswordTab();
                },
                error: function (xhr) {
                    alert('Password reset failed: ' + (xhr.responseText || 'Failed to reset password'));
                    // Only clear verification code field on failure
                    if (xhr.responseText && xhr.responseText.includes('Invalid verification code')) {
                        $('#reset-code').val('');
                    }
                },
                complete: function () {
                    submitButton.text(originalText);
                    submitButton.prop('disabled', false);
                }
            });
        }
    });
});
