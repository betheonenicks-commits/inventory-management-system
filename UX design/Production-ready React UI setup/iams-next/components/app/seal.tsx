/** IAMS brand seal — the signature verification mark. */
export function Seal({ size = 26 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 46 46" fill="none" aria-hidden="true">
      <circle cx="23" cy="23" r="21" stroke="#3A4566" strokeWidth="1.4" />
      <circle cx="23" cy="23" r="15.5" stroke="#0E7C6B" strokeWidth="1.6" />
      <path
        d="M16 23.5L20.5 28L31 16.5"
        stroke="#0E7C6B"
        strokeWidth="2.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
