export interface ProductVariant {
  id: string;
  name: string;
  colorName: string;
  colorHex: string;
  sku: string;
  stockQuantity: number;
  priceOffset: number;
  imageUrl: string;
}

export interface Product {
  id: string;
  name: string;
  tagline: string;
  description: string;
  category: 'Keyboards' | 'Workstations' | 'Haptics' | 'Audio' | 'Displays';
  basePrice: number;
  salePrice?: number;
  inStock: boolean;
  isSale: boolean;
  rating: number;
  reviewCount: number;
  images: string[];
  variants: ProductVariant[];
  features: string[];
  specs: Record<string, string>;
}

export interface CartItem {
  id: string; // unique key: `${productId}-${variantId}`
  product: Product;
  selectedVariant: ProductVariant;
  quantity: number;
  unitPrice: number;
}
