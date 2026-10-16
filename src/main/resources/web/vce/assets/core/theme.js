// Theme colors are semantic CSS tokens; operational status colors stay independent.
export function applyThemeHue(hue, root = document.documentElement) {
  if (Number.isInteger(hue) && hue >= 0 && hue <= 359) {
    root.style.setProperty('--theme-hue', String(hue));
    root.setAttribute('data-custom-hue', '');
  } else {
    root.removeAttribute('data-custom-hue');
    root.style.removeProperty('--theme-hue');
  }
}
