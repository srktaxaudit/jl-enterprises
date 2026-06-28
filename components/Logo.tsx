// JL Enterprises logo — scalable SVG recreation of the brand artwork
// (cyan "JL" + orbit, cyan→lime "ENTERPRISES" wordmark).
// To use the exact supplied PNG instead, drop it at public/logo.png and
// swap this component for <img src="/logo.png" />.

export function LogoMark({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 160 130" className={className} xmlns="http://www.w3.org/2000/svg" role="img" aria-label="JL Enterprises">
      <defs>
        <linearGradient id="jlCy" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#6beaf7" />
          <stop offset="1" stopColor="#15a8da" />
        </linearGradient>
      </defs>
      <ellipse cx="80" cy="62" rx="64" ry="34" fill="none" stroke="url(#jlCy)" strokeWidth="9" transform="rotate(-18 80 62)" strokeLinecap="round" />
      <text x="80" y="88" textAnchor="middle" fontFamily="'Segoe UI',Arial,sans-serif" fontWeight="800" fontStyle="italic" fontSize="78" fill="url(#jlCy)">JL</text>
    </svg>
  );
}

export function LogoFull({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 400 220" className={className} xmlns="http://www.w3.org/2000/svg" role="img" aria-label="JL Enterprises">
      <defs>
        <linearGradient id="jlCy2" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#6beaf7" />
          <stop offset="1" stopColor="#15a8da" />
        </linearGradient>
        <linearGradient id="entG" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#3fdcf0" />
          <stop offset="1" stopColor="#aef05a" />
        </linearGradient>
      </defs>
      <ellipse cx="200" cy="70" rx="80" ry="40" fill="none" stroke="url(#jlCy2)" strokeWidth="9" transform="rotate(-18 200 70)" strokeLinecap="round" />
      <text x="200" y="98" textAnchor="middle" fontFamily="'Segoe UI',Arial,sans-serif" fontWeight="800" fontStyle="italic" fontSize="88" fill="url(#jlCy2)">JL</text>
      <text x="200" y="188" textAnchor="middle" fontFamily="'Arial Black',Impact,sans-serif" fontWeight="800" fontSize="58" letterSpacing="1" fill="url(#entG)" textLength="370" lengthAdjust="spacingAndGlyphs">ENTERPRISES</text>
    </svg>
  );
}
