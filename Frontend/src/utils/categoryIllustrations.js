const palettes = [
  { bg: '#fef3c7', accent: '#d97706', icon: '#f59e0b' },
  { bg: '#fee2e2', accent: '#dc2626', icon: '#ef4444' },
  { bg: '#fce7f3', accent: '#db2777', icon: '#ec4899' },
  { bg: '#d1fae5', accent: '#059669', icon: '#10b981' },
  { bg: '#cffafe', accent: '#0891b2', icon: '#06b6d4' },
  { bg: '#ede9fe', accent: '#6d28d9', icon: '#7c3aed' },
  { bg: '#e0e7ff', accent: '#4f46e5', icon: '#6366f1' },
  { bg: '#ccfbf1', accent: '#0f766e', icon: '#14b8a6' },
  { bg: '#dcfce7', accent: '#16a34a', icon: '#22c55e' },
  { bg: '#fce7f3', accent: '#be185d', icon: '#f472b6' },
  { bg: '#f3e8ff', accent: '#7c3aed', icon: '#8b5cf6' },
  { bg: '#f3f4f6', accent: '#4b5563', icon: '#6b7280' },
]

const categoryLabels = {
  VITAMINS: 'Vitamins\n& Supplements',
  PAIN_RELIEF: 'Pain\nRelief',
  SKIN_CARE: 'Skin\nCare',
  DIGESTIVE_HEALTH: 'Digestive\nHealth',
  RESPIRATORY: 'Respiratory\nCare',
  HEART_HEALTH: 'Heart\nHealth',
  DIABETES_CARE: 'Diabetes\nCare',
  FIRST_AID: 'First\nAid',
  MEDICAL_DEVICES: 'Medical\nDevices',
  PERSONAL_CARE: 'Personal\nCare',
  WELLNESS: 'Wellness',
  BABY_CARE: 'Baby\nCare',
  ELDERLY_CARE: 'Elderly\nCare',
  OTHER: 'Other',
}

const categoryIcons = {
  VITAMINS: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <rect x="35" y="20" width="50" height="80" rx="25" fill="${p.icon}" opacity="0.2"/>
      <rect x="38" y="23" width="44" height="74" rx="22" fill="white" stroke="${p.accent}" stroke-width="3"/>
      <line x1="60" y1="23" x2="60" y2="97" stroke="${p.accent}" stroke-width="2.5" opacity="0.5"/>
      <rect x="21" y="45" width="18" height="50" rx="9" fill="${p.icon}" opacity="0.3"/>
      <circle cx="30" cy="55" r="4" fill="${p.accent}"/>
      <circle cx="30" cy="70" r="4" fill="${p.accent}"/>
      <circle cx="30" cy="85" r="4" fill="${p.accent}"/>
      <rect x="86" y="40" width="18" height="15" rx="7" fill="${p.icon}" opacity="0.3"/>
      <rect x="86" y="60" width="18" height="15" rx="7" fill="${p.icon}" opacity="0.3"/>
      <circle cx="95" cy="47" r="3" fill="${p.accent}"/>
      <circle cx="95" cy="67" r="3" fill="${p.accent}"/>
    `
  },
  PAIN_RELIEF: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <circle cx="60" cy="65" r="45" fill="${p.icon}" opacity="0.15"/>
      <polygon points="60,15 35,70 55,70 50,120 75,65 55,65 65,15" fill="${p.accent}" opacity="0.8" stroke="${p.accent}" stroke-width="3" stroke-linejoin="round"/>
      <circle cx="60" cy="60" r="12" fill="white" opacity="0.6"/>
    `
  },
  SKIN_CARE: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <path d="M60 15 C30 25 15 55 15 85 C15 110 35 125 60 125 C85 125 105 110 105 85 C105 55 90 25 60 15Z" fill="${p.icon}" opacity="0.2" stroke="${p.accent}" stroke-width="2.5"/>
      <circle cx="45" cy="70" r="8" fill="${p.accent}" opacity="0.15"/>
      <circle cx="75" cy="70" r="8" fill="${p.accent}" opacity="0.15"/>
      <circle cx="60" cy="95" r="6" fill="${p.accent}" opacity="0.15"/>
      <circle cx="60" cy="55" r="5" fill="${p.accent}" opacity="0.3"/>
    `
  },
  DIGESTIVE_HEALTH: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <ellipse cx="60" cy="70" rx="40" ry="30" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <path d="M45 80 Q60 95 75 80" fill="none" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <circle cx="42" cy="60" r="4" fill="${p.accent}"/>
      <circle cx="78" cy="60" r="4" fill="${p.accent}"/>
      <rect x="52" y="25" width="16" height="20" rx="8" fill="${p.icon}" opacity="0.3" stroke="${p.accent}" stroke-width="2"/>
      <line x1="60" y1="45" x2="60" y2="55" stroke="${p.accent}" stroke-width="2"/>
    `
  },
  RESPIRATORY: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <path d="M30 30 C15 20 10 45 15 65 C20 85 40 100 60 100 C80 100 100 85 105 65 C110 45 105 20 90 30" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <path d="M40 50 C25 40 20 55 25 70 C30 85 55 95 60 95 C65 95 90 85 95 70 C100 55 95 40 80 50" fill="none" stroke="${p.accent}" stroke-width="2" opacity="0.5"/>
      <line x1="45" y1="105" x2="45" y2="125" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <line x1="75" y1="105" x2="75" y2="125" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <line x1="40" y1="125" x2="80" y2="125" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round"/>
    `
  },
  HEART_HEALTH: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <path d="M60 120 C30 95 10 65 20 45 C30 25 50 25 60 40 C70 25 90 25 100 45 C110 65 90 95 60 120Z" fill="${p.icon}" opacity="0.2" stroke="${p.accent}" stroke-width="3"/>
      <path d="M40 60 L50 75 L55 65 L60 80 L65 55 L70 70 L80 60" fill="none" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" opacity="0.7"/>
    `
  },
  DIABETES_CARE: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <circle cx="60" cy="65" r="40" fill="${p.icon}" opacity="0.1" stroke="${p.accent}" stroke-width="2.5"/>
      <path d="M60 20 L60 50" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <circle cx="60" cy="65" r="15" fill="${p.icon}" opacity="0.2" stroke="${p.accent}" stroke-width="2"/>
      <circle cx="60" cy="65" r="6" fill="${p.accent}"/>
      <line x1="60" y1="80" x2="60" y2="110" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <rect x="40" y="110" width="40" height="15" rx="7" fill="${p.icon}" opacity="0.2" stroke="${p.accent}" stroke-width="2"/>
      <text x="60" y="121" text-anchor="middle" font-size="9" fill="${p.accent}" font-weight="bold">mg/dL</text>
    `
  },
  FIRST_AID: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <rect x="25" y="35" width="70" height="70" rx="15" fill="white" stroke="${p.accent}" stroke-width="3"/>
      <line x1="60" y1="48" x2="60" y2="92" stroke="${p.accent}" stroke-width="4" stroke-linecap="round"/>
      <line x1="48" y1="70" x2="72" y2="70" stroke="${p.accent}" stroke-width="4" stroke-linecap="round"/>
      <circle cx="60" cy="70" r="22" fill="${p.icon}" opacity="0.1"/>
    `
  },
  MEDICAL_DEVICES: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <rect x="20" y="25" width="80" height="55" rx="8" fill="white" stroke="${p.accent}" stroke-width="3"/>
      <rect x="28" y="33" width="64" height="39" rx="4" fill="${p.icon}" opacity="0.1"/>
      <path d="M30 55 L55 35 L70 50 L90 40" fill="none" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
      <circle cx="55" cy="35" r="3" fill="${p.accent}"/>
      <rect x="48" y="80" width="24" height="5" rx="2" fill="${p.icon}" opacity="0.4"/>
      <rect x="45" y="88" width="30" height="15" rx="4" fill="${p.icon}" opacity="0.2" stroke="${p.accent}" stroke-width="2"/>
      <rect x="52" y="106" width="16" height="20" rx="3" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2"/>
    `
  },
  PERSONAL_CARE: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <circle cx="80" cy="35" r="20" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <circle cx="80" cy="35" r="8" fill="${p.accent}" opacity="0.3"/>
      <path d="M65 20 L55 60 C50 75 40 85 45 100 C50 115 70 115 75 100 C80 90 85 70 80 55 L70 20" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <circle cx="40" cy="45" r="12" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <circle cx="40" cy="45" r="5" fill="${p.accent}" opacity="0.3"/>
      <path d="M28 42 L20 72 C18 80 22 90 30 92 C38 94 42 86 40 75 L35 58" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <path d="M30 92 L35 58" fill="none" stroke="${p.accent}" stroke-width="2" opacity="0.5"/>
    `
  },
  WELLNESS: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <path d="M60 120 C20 100 20 50 60 20 C100 50 100 100 60 120Z" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <path d="M60 30 L60 110" stroke="${p.accent}" stroke-width="2" opacity="0.4"/>
      <path d="M60 55 C45 45 45 30 60 35 C75 30 75 45 60 55Z" fill="${p.accent}" opacity="0.5"/>
      <path d="M60 80 C40 85 35 100 45 110 C55 120 65 120 75 110 C85 100 80 85 60 80Z" fill="${p.accent}" opacity="0.3"/>
    `
  },
  BABY_CARE: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <circle cx="60" cy="50" r="35" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <circle cx="45" cy="40" r="4" fill="${p.accent}"/>
      <circle cx="75" cy="40" r="4" fill="${p.accent}"/>
      <path d="M48 60 Q60 72 72 60" fill="none" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round"/>
      <circle cx="50" cy="27" r="6" fill="${p.icon}" opacity="0.2" stroke="${p.accent}" stroke-width="2"/>
      <circle cx="70" cy="27" r="6" fill="${p.icon}" opacity="0.2" stroke="${p.accent}" stroke-width="2"/>
      <path d="M60 85 L60 120" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round"/>
      <path d="M40 100 L80 100" stroke="${p.accent}" stroke-width="2" stroke-linecap="round" opacity="0.5"/>
    `
  },
  ELDERLY_CARE: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <circle cx="60" cy="40" r="25" fill="${p.icon}" opacity="0.15" stroke="${p.accent}" stroke-width="2.5"/>
      <circle cx="52" cy="33" r="4" fill="${p.accent}"/>
      <circle cx="68" cy="33" r="4" fill="${p.accent}"/>
      <path d="M48 48 Q60 58 72 48" fill="none" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round"/>
      <path d="M60 65 L60 120" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <path d="M35 90 L85 90" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round" opacity="0.5"/>
      <line x1="82" y1="30" x2="100" y2="20" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <line x1="100" y1="20" x2="105" y2="10" stroke="${p.accent}" stroke-width="2" stroke-linecap="round"/>
      <circle cx="105" cy="10" r="4" fill="${p.icon}" opacity="0.3" stroke="${p.accent}" stroke-width="2"/>
    `
  },
  OTHER: {
    viewBox: '0 0 120 140',
    getSvg: (p) => `
      <rect x="25" y="35" width="70" height="70" rx="12" fill="white" stroke="${p.accent}" stroke-width="3"/>
      <circle cx="45" cy="55" r="8" fill="${p.icon}" opacity="0.2"/>
      <circle cx="45" cy="55" r="4" fill="${p.accent}"/>
      <path d="M62 55 L80 55" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <path d="M62 70 L80 70" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <path d="M62 85 L72 85" stroke="${p.accent}" stroke-width="3" stroke-linecap="round"/>
      <path d="M45 55 L45 95" stroke="${p.accent}" stroke-width="2.5" stroke-linecap="round" opacity="0.5"/>
    `
  },
}

export function generateCategorySvg(category, paletteIndex = null) {
  const idx = paletteIndex !== null ? paletteIndex : Object.keys(categoryLabels).indexOf(category)
  const palette = palettes[idx % palettes.length]
  const icon = categoryIcons[category] || categoryIcons.OTHER

  return `data:image/svg+xml,${encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="${icon.viewBox}" width="240" height="240">
      <rect width="240" height="240" rx="24" fill="${palette.bg}"/>
      <g transform="translate(60, 45)">
        ${icon.getSvg({ accent: palette.accent, icon: palette.icon })}
      </g>
      <text x="120" y="200" text-anchor="middle" font-family="system-ui, sans-serif" font-size="20" font-weight="700" fill="${palette.accent}">
        ${(categoryLabels[category] || category).split('\n').map((line, i) =>
          `<tspan x="120" dy="${i === 0 ? 0 : 24}">${line}</tspan>`
        ).join('')}
      </text>
    </svg>
  `.trim())}`
}

export function getCategoryPalette(category) {
  const idx = Object.keys(categoryLabels).indexOf(category)
  return palettes[idx % palettes.length]
}
