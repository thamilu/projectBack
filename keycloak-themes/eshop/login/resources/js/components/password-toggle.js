(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {
    var buttons = document.querySelectorAll('[data-action="toggle-password"]');
    
    buttons.forEach(function (button) {
      button.addEventListener('click', function () {
        var targetId = button.getAttribute('data-target');
        var input = document.getElementById(targetId);
        if (!input) return;

        var isPassword = input.type === 'password';
        input.type = isPassword ? 'text' : 'password';

        // Update ARIA states
        button.setAttribute('aria-pressed', String(!isPassword));
        button.setAttribute(
          'aria-label',
          isPassword ? 'Hide password' : 'Show password'
        );

        // Toggle SVGs
        var eyeIcon = button.querySelector('.kc-icon--eye');
        var eyeOffIcon = button.querySelector('.kc-icon--eye-off');
        
        if (eyeIcon) {
          eyeIcon.hidden = !isPassword;
          if (!isPassword) {
            eyeIcon.setAttribute('hidden', 'true');
          } else {
            eyeIcon.removeAttribute('hidden');
          }
        }
        
        if (eyeOffIcon) {
          eyeOffIcon.hidden = isPassword;
          if (isPassword) {
            eyeOffIcon.setAttribute('hidden', 'true');
          } else {
            eyeOffIcon.removeAttribute('hidden');
          }
        }
      });
    });
  });
})();
