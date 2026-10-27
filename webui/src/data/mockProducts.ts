import type { Product } from '../types/product';

export const MOCK_PRODUCTS: Product[] = [
  {
    id: 'prod-001',
    name: 'Aether Split 75 CNC Keyboard',
    tagline: 'Machined aerospace aluminum split mechanical keyboard with OLED telemetry.',
    description: 'Engineered for ergonomic endurance. Dual-chassis CNC aluminum unibody with magnetic tenting pucks, hot-swap PCB, and per-key RGB diffuser arrays. Tuned for zero flex and pure acoustic thock.',
    category: 'Keyboards',
    basePrice: 429,
    salePrice: 389,
    inStock: true,
    isSale: true,
    rating: 4.9,
    reviewCount: 142,
    images: [
      'https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=1000&q=80'
    ],
    variants: [
      {
        id: 'var-001-obsidian',
        name: 'Obsidian Black',
        colorName: 'Deep Obsidian',
        colorHex: '#18181b',
        sku: 'AETH-75-OBS',
        stockQuantity: 18,
        priceOffset: 0,
        imageUrl: 'https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=1000&q=80'
      },
      {
        id: 'var-001-silver',
        name: 'Raw Anodized Silver',
        colorName: 'Chrono Silver',
        colorHex: '#e4e4e7',
        sku: 'AETH-75-SLV',
        stockQuantity: 9,
        priceOffset: 20,
        imageUrl: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1000&q=80'
      },
      {
        id: 'var-001-emerald',
        name: 'Emerald Forest Edition',
        colorName: 'Cyber Emerald',
        colorHex: '#059669',
        sku: 'AETH-75-EMR',
        stockQuantity: 4,
        priceOffset: 35,
        imageUrl: 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=1000&q=80'
      }
    ],
    features: [
      'Dual 32-bit ARM Cortex-M4 Microcontrollers',
      '1.4-inch OLED Status & WPM Telemetry Display',
      'QMK / VIA open firmware programmable layers',
      'Gasket-mounted carbon fiber switch plate'
    ],
    specs: {
      'Weight': '1,840g',
      'Connectivity': 'USB-C / 2.4GHz Wireless / Bluetooth 5.2',
      'Battery Life': 'Up to 120 hours without RGB',
      'Layout': '75% Ortholinear Split (82 keys)'
    }
  },
  {
    id: 'prod-002',
    name: 'Vanguard Cyberdeck Terminal Pro',
    tagline: 'Portable mechanical cyberdeck with ultrawide bar touchscreen.',
    description: 'Field terminal built into an IP67 Pelican chassis. Equipped with an 8.8-inch stretched 1920x480 IPS panel, integrated battery BMS, programmable macro matrix, and modular SDR radio bay.',
    category: 'Workstations',
    basePrice: 899,
    salePrice: 849,
    inStock: true,
    isSale: true,
    rating: 4.8,
    reviewCount: 68,
    images: [
      'https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1000&q=80'
    ],
    variants: [
      {
        id: 'var-002-matte-black',
        name: 'Tactical Stealth',
        colorName: 'Matte Vantablack',
        colorHex: '#09090b',
        sku: 'CYB-PRO-BLK',
        stockQuantity: 6,
        priceOffset: 0,
        imageUrl: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=1000&q=80'
      },
      {
        id: 'var-002-cyber-orange',
        name: 'Hazard Orange Edition',
        colorName: 'Safety Amber',
        colorHex: '#ea580c',
        sku: 'CYB-PRO-ORG',
        stockQuantity: 2,
        priceOffset: 50,
        imageUrl: 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1000&q=80'
      }
    ],
    features: [
      '8.8" 60Hz 1920x480 Ultrawide IPS Touchscreen',
      'Integrated 96Wh flight-safe hot-swap battery',
      'Direct GPIO breakout and SMA RF antenna terminals',
      'Mechanical ortholinear 40% keyboard with Kailh Choc V2'
    ],
    specs: {
      'Weight': '2,400g',
      'Compute': 'Carrier board compatible with CM4 / RK3588',
      'I/O': 'Dual Gigabit Ethernet, 3x USB 3.2, MicroHDMI Out',
      'Enclosure': 'Carbon-reinforced Polycarbonate & Billet 6061'
    }
  },
  {
    id: 'prod-003',
    name: 'Haptic Dial Precision Surface',
    tagline: 'Brushless motor force-feedback rotary dial for timeline scrub & CAD control.',
    description: 'Experience software through haptic detents. Programmable magnetic virtual detents simulate friction, spring tension, and ratchet clicks for video scrubbing, audio mastering, and 3D modeling.',
    category: 'Haptics',
    basePrice: 289,
    salePrice: undefined,
    inStock: true,
    isSale: false,
    rating: 5.0,
    reviewCount: 94,
    images: [
      'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=1000&q=80'
    ],
    variants: [
      {
        id: 'var-003-space-gray',
        name: 'Space Tungsten',
        colorName: 'Tungsten Gray',
        colorHex: '#27272a',
        sku: 'HAPT-DIAL-GRY',
        stockQuantity: 22,
        priceOffset: 0,
        imageUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80'
      },
      {
        id: 'var-003-champagne',
        name: 'Raw Brass Core',
        colorName: 'Brushed Brass',
        colorHex: '#ca8a04',
        sku: 'HAPT-DIAL-BRS',
        stockQuantity: 5,
        priceOffset: 45,
        imageUrl: 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=1000&q=80'
      }
    ],
    features: [
      'Brushless BLDC Motor with zero backlash torque control',
      'Sub-degree optical angle encoder with 4096 CPR',
      'Round 1.28-inch circular 240x240 LCD top cap',
      'Native DaVinci Resolve, Premiere & Blender integrations'
    ],
    specs: {
      'Torque': 'Max 0.35 N·m continuous force feedback',
      'Connectivity': 'USB-C with low latency HID profile (1000Hz)',
      'Dimensions': '88mm x 88mm x 54mm',
      'Materials': 'CNC bead-blasted 6063 Aluminum & PVD glass'
    }
  },
  {
    id: 'prod-004',
    name: 'Monolith Studio Reference Nearfield',
    tagline: 'Active DSP beryllium studio monitors with coaxial waveguide.',
    description: 'Precision acoustic engineering. Pure beryllium dome tweeters housed inside a point-source coaxial waveguide, powered by twin 250W Hypex NCore Class-D amplifiers with 96kHz FIR phase correction.',
    category: 'Audio',
    basePrice: 1199,
    salePrice: 1049,
    inStock: false,
    isSale: true,
    rating: 4.9,
    reviewCount: 31,
    images: [
      'https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=1000&q=80'
    ],
    variants: [
      {
        id: 'var-004-matte-graphite',
        name: 'Cast Mineral Graphite',
        colorName: 'Graphite',
        colorHex: '#1c1917',
        sku: 'MONO-REF-GRPH',
        stockQuantity: 0,
        priceOffset: 0,
        imageUrl: 'https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=1000&q=80'
      },
      {
        id: 'var-004-pure-chalk',
        name: 'Cast Mineral Chalk',
        colorName: 'White Chalk',
        colorHex: '#fafaf9',
        sku: 'MONO-REF-WHT',
        stockQuantity: 0,
        priceOffset: 50,
        imageUrl: 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=1000&q=80'
      }
    ],
    features: [
      '1" Pure Beryllium dome coaxial high-frequency transducer',
      '6.5" Woven carbon fiber mid-bass woofer',
      'Dual Hypex NCore 250W RMS amplifiers (500W total/channel)',
      'Digital AES/EBU, Dante networked audio, and balanced XLR'
    ],
    specs: {
      'Frequency Response': '38Hz – 42kHz (±1.5dB)',
      'Max SPL': '114 dB peak @ 1m',
      'Crossover': 'Linear-phase FIR DSP crossover @ 1,650Hz',
      'Weight': '11.8kg per monitor'
    }
  }
];
