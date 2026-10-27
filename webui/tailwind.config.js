/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#059669',
          50: '#ecfdf5',
          100: '#d1fae5',
          500: '#10b981',
          600: '#059669',
          700: '#047857',
        },
        secondary: {
          DEFAULT: '#10B981',
        },
        accent: {
          DEFAULT: '#EA580C',
          500: '#f97316',
          600: '#ea580c',
        },
        oled: '#050505',
        surface: {
          dark: '#0C0C0C',
          card: '#121212',
        }
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', '"Nunito Sans"', 'system-ui', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', 'sans-serif'],
      },
      borderRadius: {
        'bezel-outer': '2rem',
        'bezel-inner': 'calc(2rem - 0.5rem)',
      },
      transitionTimingFunction: {
        'fluid': 'cubic-bezier(0.32, 0.72, 0, 1)',
      }
    },
  },
  plugins: [],
}
