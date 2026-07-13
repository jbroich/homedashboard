/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        dashboard: {
          background: '#F7F4EF',
          surface: '#FFFFFF',
          primary: '#0F766E',
          temperature: '#E76F51',
          humidity: '#3A86FF',
          warning: '#F59E0B',
          text: '#1F2933',
          muted: '#667085',
          border: '#E2E8F0',
          soft: '#EEF6F4',
        },
      },
      borderRadius: {
        card: '8px',
      },
    },
  },
  plugins: [],
};
