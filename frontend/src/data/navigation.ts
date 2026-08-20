import type { AssetType } from "@/types/api";

export type ProductMenuItem = {
  assetType?: AssetType;
  currency?: string;
  description: string;
  key: string;
  label: string;
  sectionId: "assets" | "transactions";
};

export const productMenuItems = [
  {
    assetType: "STOCK",
    currency: "TRY",
    description: "TRY bazlı hisse varlıklarını hızlıca incele.",
    key: "borsa-istanbul",
    label: "Borsa İstanbul",
    sectionId: "assets",
  },
  {
    assetType: "STOCK",
    currency: "USD",
    description: "USD bazlı hisse varlıklarını ve pozisyonlarını öne çıkar.",
    key: "abd-borsalari",
    label: "ABD Borsaları",
    sectionId: "assets",
  },
  {
    description: "Fon takibi için sonraki domain adımında ayrı varlık tipi eklenebilir.",
    key: "yatirim-fonlari",
    label: "Yatırım Fonları",
    sectionId: "assets",
  },
  {
    assetType: "CRYPTO",
    description: "Kripto varlıkları tek tıkla filtrele.",
    key: "kripto",
    label: "Kripto",
    sectionId: "assets",
  },
  {
    assetType: "STOCK",
    currency: "EUR",
    description: "EUR bazlı Avrupa hisse varlıklarını filtrele.",
    key: "avrupa-borsalari",
    label: "Avrupa Borsaları",
    sectionId: "assets",
  },
  {
    description: "Türev ürün işlemleri için işlem geçmişi bölümünü odakta aç.",
    key: "viop",
    label: "VİOP",
    sectionId: "transactions",
  },
  {
    description: "Opsiyon desteği için ileride ayrı ürün tipi ve işlem kuralları eklenebilir.",
    key: "opsiyon",
    label: "Opsiyon",
    sectionId: "transactions",
  },
  {
    description: "Varant takibi için sonraki domain adımında ayrı varlık tipi eklenebilir.",
    key: "varantlar",
    label: "Varantlar",
    sectionId: "assets",
  },
] satisfies ProductMenuItem[];

export const dashboardSections = [
  { href: "#overview", label: "Genel" },
  { href: "#summary", label: "Özet" },
  { href: "#portfolio", label: "Portföy" },
  { href: "#assets", label: "Varlıklar" },
  { href: "#transactions", label: "İşlemler" },
  { href: "#positions", label: "Pozisyonlar" },
];
