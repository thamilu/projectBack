(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('kc-form-login');
    var submitBtn = document.getElementById('kc-login');

    if (!form || !submitBtn) return;

    // Remove native browser validation UI in favour of accessible inline errors
    form.setAttribute('novalidate', '');

    var btnText = submitBtn.querySelector('.kc-login-button__text');
    var spinner = submitBtn.querySelector('.kc-spinner');
    var loadingText = submitBtn.getAttribute('data-loading-text') || 'Signing in...';
    var defaultText = submitBtn.getAttribute('data-default-text') || 'Sign In';
    var SAFETY_TIMEOUT_MS = 15000;
    var safetyTimer = null;
    var isSubmitting = false;

    function setLoadingState(isLoading) {
      submitBtn.setAttribute('aria-busy', String(isLoading));
      if (btnText) btnText.textContent = isLoading ? loadingText : defaultText;
      if (spinner) {
        spinner.hidden = !isLoading;
        if (isLoading) {
          spinner.removeAttribute('hidden');
        } else {
          spinner.setAttribute('hidden', 'true');
        }
      }
      var overlay = document.getElementById('kc-loading-overlay');
      if (overlay) {
        overlay.hidden = !isLoading;
      }
    }


    function resetState() {
      isSubmitting = false;
      setLoadingState(false);
      if (safetyTimer) {
        clearTimeout(safetyTimer);
        safetyTimer = null;
      }
    }

    function showClientError(input, errorId, message) {
      input.setAttribute('aria-invalid', 'true');
      input.setAttribute('aria-describedby', errorId);

      input.classList.add('pf-m-error');
      var group = input.closest('.pf-c-form__group');
      if (group) {
        group.classList.add('pf-m-error');
      }

      var errorEl = document.getElementById(errorId);
      if (errorEl) {
        var textEl = errorEl.querySelector('.kc-error-message__text');
        if (textEl) {
          textEl.textContent = message;
        } else {
          errorEl.textContent = message;
        }
        errorEl.hidden = false;
      }
    }

    function clearClientError(input, errorId) {
      input.setAttribute('aria-invalid', 'false');
      input.classList.remove('pf-m-error');
      
      var group = input.closest('.pf-c-form__group');
      if (group) {
        group.classList.remove('pf-m-error');
      }

      var errorEl = document.getElementById(errorId);
      if (errorEl) {
        errorEl.hidden = true;
        var textEl = errorEl.querySelector('.kc-error-message__text');
        if (textEl) textEl.textContent = '';
      }
    }

    form.addEventListener('submit', function (event) {
      if (isSubmitting) {
        event.preventDefault();
        return;
      }

      var username = document.getElementById('username');
      var password = document.getElementById('password');
      var hasError = false;
      var firstInvalidField = null;

      // Validate Username
      if (username) {
        var usernameVal = username.value.trim();
        if (!usernameVal) {
          var userMsg = username.getAttribute('data-required-msg') || 'Email or username is required';
          showClientError(username, 'username-error', userMsg);
          hasError = true;
          firstInvalidField = username;
        } else if (username.type === 'email' || (usernameVal.indexOf('@') !== -1)) {
          var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
          if (!emailRegex.test(usernameVal)) {
            showClientError(username, 'username-error', 'Please enter a valid email address.');
            hasError = true;
            firstInvalidField = username;
          } else {
            clearClientError(username, 'username-error');
          }
        } else {
          clearClientError(username, 'username-error');
        }
      }

      // Validate Password
      if (password) {
        if (!password.value.trim()) {
          var passMsg = password.getAttribute('data-required-msg') || 'Password is required';
          showClientError(password, 'password-error', passMsg);
          hasError = true;
          if (!firstInvalidField) firstInvalidField = password;
        } else {
          clearClientError(password, 'password-error');
        }
      }

      if (hasError) {
        event.preventDefault();
        if (firstInvalidField) {
          firstInvalidField.focus();
        }
        return;
      }

      isSubmitting = true;
      setLoadingState(true);

      // Safety reset — prevents permanent disabled state if server hangs/fails
      safetyTimer = setTimeout(resetState, SAFETY_TIMEOUT_MS);
    });

    // Reset loading state if browser navigates back (bfcache optimization)
    window.addEventListener('pageshow', function (event) {
      if (event.persisted) {
        resetState();
      }
    });

    // General overlay submit handler for other forms (e.g. reset-password)
    var otherForms = document.querySelectorAll('form');
    Array.prototype.forEach.call(otherForms, function(f) {
      if (f.id === 'kc-form-login') return; // Handled specifically
      f.addEventListener('submit', function(e) {
        if (f.checkValidity && !f.checkValidity()) {
          return; // Don't show overlay if HTML5 validation fails
        }
        var overlay = document.getElementById('kc-loading-overlay');
        if (overlay) {
          overlay.hidden = false;
        }
      });
    });
  });
})();
