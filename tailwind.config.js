/** @type {import('tailwindcss').Config} */
module.exports = {
  // NOTE: We match files under src/app since the expo routing root is set to src/app.
  content: [
    "./src/app/**/*.{js,jsx,ts,tsx}",
    "./src/components/**/*.{js,jsx,ts,tsx}",
    "./modules/whatsapp-notification-listener/**/*.{js,jsx,ts,tsx}"
  ],
  presets: [require("nativewind/preset")],
  theme: {
    extend: {},
  },
  plugins: [],
}
