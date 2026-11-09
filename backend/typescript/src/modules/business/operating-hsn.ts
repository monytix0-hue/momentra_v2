/**
 * HSN code lookup for GST lines.
 *
 * The HSN (Harmonized System of Nomenclature) code decides the GST rate a product attracts.
 * Operators know their product, not necessarily its code, so the capture sheet offers a search
 * that returns the chapter description and the rate the chapter usually attracts.
 *
 * This is a curated chapter-level table, not the full CBIC schedule. It exists to help an
 * operator pick the right chapter and rate; it is not a legal classification. The returned
 * `gstRate` is a sensible default that the operator can override on the master, and the API
 * response says so in `note`.
 */

export interface HsnEntry {
  /** HSN chapter (2 digits) or a specific 4/6/8-digit code. */
  code: string;
  description: string;
  /** Default GST rate for this chapter, as a whole percent string (`'18'`). */
  gstRate: string;
  classification: 'TAXABLE' | 'EXEMPT' | 'NIL' | 'NON_GST';
}

/** Chapters with a single, stable headline rate. Ordered by code. */
export const HSN_CHAPTERS: ReadonlyArray<HsnEntry> = [
  { code: '01', description: 'Live animals', gstRate: '0', classification: 'EXEMPT' },
  { code: '02', description: 'Meat and edible meat offal', gstRate: '0', classification: 'EXEMPT' },
  { code: '03', description: 'Fish and crustaceans', gstRate: '5', classification: 'TAXABLE' },
  { code: '04', description: 'Dairy produce, eggs, honey', gstRate: '5', classification: 'TAXABLE' },
  { code: '05', description: 'Products of animal origin, not elsewhere specified', gstRate: '5', classification: 'TAXABLE' },
  { code: '06', description: 'Live trees, plants, cut flowers', gstRate: '5', classification: 'TAXABLE' },
  { code: '07', description: 'Edible vegetables', gstRate: '0', classification: 'EXEMPT' },
  { code: '08', description: 'Edible fruit and nuts', gstRate: '0', classification: 'EXEMPT' },
  { code: '09', description: 'Coffee, tea, spices', gstRate: '5', classification: 'TAXABLE' },
  { code: '10', description: 'Cereals', gstRate: '0', classification: 'EXEMPT' },
  { code: '11', description: 'Milling products, malt, starches', gstRate: '5', classification: 'TAXABLE' },
  { code: '12', description: 'Oil seeds and oleaginous fruits', gstRate: '5', classification: 'TAXABLE' },
  { code: '13', description: 'Lac, gums, resins', gstRate: '5', classification: 'TAXABLE' },
  { code: '14', description: 'Vegetable plaiting materials', gstRate: '5', classification: 'TAXABLE' },
  { code: '15', description: 'Animal or vegetable fats and oils', gstRate: '5', classification: 'TAXABLE' },
  { code: '16', description: 'Preparations of meat, fish', gstRate: '12', classification: 'TAXABLE' },
  { code: '17', description: 'Sugar and confectionery', gstRate: '5', classification: 'TAXABLE' },
  { code: '18', description: 'Cocoa and cocoa preparations', gstRate: '18', classification: 'TAXABLE' },
  { code: '19', description: 'Preparations of cereals, flour, pastry', gstRate: '18', classification: 'TAXABLE' },
  { code: '20', description: 'Preparations of vegetables, fruit, nuts', gstRate: '12', classification: 'TAXABLE' },
  { code: '21', description: 'Miscellaneous edible preparations', gstRate: '18', classification: 'TAXABLE' },
  { code: '22', description: 'Beverages, spirits and vinegar', gstRate: '18', classification: 'TAXABLE' },
  { code: '23', description: 'Residues and waste from the food industries, animal feed', gstRate: '5', classification: 'TAXABLE' },
  { code: '24', description: 'Tobacco and manufactured tobacco substitutes', gstRate: '28', classification: 'TAXABLE' },
  { code: '25', description: 'Salt, sulphur, earths and stone, plaster, lime, cement', gstRate: '5', classification: 'TAXABLE' },
  { code: '26', description: 'Ores, slag and ash', gstRate: '5', classification: 'TAXABLE' },
  { code: '27', description: 'Mineral fuels, oils, distillation products', gstRate: '18', classification: 'TAXABLE' },
  { code: '28', description: 'Inorganic chemicals, precious metal compounds', gstRate: '18', classification: 'TAXABLE' },
  { code: '29', description: 'Organic chemicals', gstRate: '18', classification: 'TAXABLE' },
  { code: '30', description: 'Pharmaceutical products', gstRate: '12', classification: 'TAXABLE' },
  { code: '31', description: 'Fertilisers', gstRate: '5', classification: 'TAXABLE' },
  { code: '32', description: 'Tanning and dyeing extracts, paints, varnishes', gstRate: '18', classification: 'TAXABLE' },
  { code: '33', description: 'Essential oils, perfumery, cosmetics', gstRate: '18', classification: 'TAXABLE' },
  { code: '34', description: 'Soap, washing preparations, waxes', gstRate: '18', classification: 'TAXABLE' },
  { code: '35', description: 'Albuminoidal substances, glues, enzymes', gstRate: '18', classification: 'TAXABLE' },
  { code: '36', description: 'Explosives, pyrotechnics, matches', gstRate: '18', classification: 'TAXABLE' },
  { code: '37', description: 'Photographic and cinematographic goods', gstRate: '18', classification: 'TAXABLE' },
  { code: '38', description: 'Miscellaneous chemical products', gstRate: '18', classification: 'TAXABLE' },
  { code: '39', description: 'Plastics and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '40', description: 'Rubber and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '41', description: 'Raw hides and skins, leather', gstRate: '5', classification: 'TAXABLE' },
  { code: '42', description: 'Articles of leather, saddlery, travel goods', gstRate: '18', classification: 'TAXABLE' },
  { code: '43', description: 'Furskins and artificial fur', gstRate: '18', classification: 'TAXABLE' },
  { code: '44', description: 'Wood and articles of wood, charcoal', gstRate: '18', classification: 'TAXABLE' },
  { code: '45', description: 'Cork and articles of cork', gstRate: '18', classification: 'TAXABLE' },
  { code: '46', description: 'Manufactures of straw, basketware', gstRate: '18', classification: 'TAXABLE' },
  { code: '47', description: 'Pulp of wood, recovered paper', gstRate: '12', classification: 'TAXABLE' },
  { code: '48', description: 'Paper and paperboard, articles thereof', gstRate: '12', classification: 'TAXABLE' },
  { code: '49', description: 'Printed books, newspapers, pictures', gstRate: '0', classification: 'EXEMPT' },
  { code: '50', description: 'Silk', gstRate: '5', classification: 'TAXABLE' },
  { code: '51', description: 'Wool, fine or coarse animal hair', gstRate: '5', classification: 'TAXABLE' },
  { code: '52', description: 'Cotton', gstRate: '5', classification: 'TAXABLE' },
  { code: '53', description: 'Other vegetable textile fibres, paper yarn', gstRate: '5', classification: 'TAXABLE' },
  { code: '54', description: 'Man-made filaments', gstRate: '12', classification: 'TAXABLE' },
  { code: '55', description: 'Man-made staple fibres', gstRate: '12', classification: 'TAXABLE' },
  { code: '56', description: 'Wadding, felt, nonwovens, ropes, twine', gstRate: '12', classification: 'TAXABLE' },
  { code: '57', description: 'Carpets and other textile floor coverings', gstRate: '12', classification: 'TAXABLE' },
  { code: '58', description: 'Special woven fabrics, lace, embroidery', gstRate: '12', classification: 'TAXABLE' },
  { code: '59', description: 'Impregnated, coated, laminated textile fabrics', gstRate: '12', classification: 'TAXABLE' },
  { code: '60', description: 'Knitted or crocheted fabrics', gstRate: '5', classification: 'TAXABLE' },
  { code: '61', description: 'Articles of apparel, knitted or crocheted', gstRate: '5', classification: 'TAXABLE' },
  { code: '62', description: 'Articles of apparel, not knitted', gstRate: '5', classification: 'TAXABLE' },
  { code: '63', description: 'Other made-up textile articles, sets, worn clothing', gstRate: '5', classification: 'TAXABLE' },
  { code: '64', description: 'Footwear, gaiters and the like', gstRate: '18', classification: 'TAXABLE' },
  { code: '65', description: 'Headgear and parts thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '66', description: 'Umbrellas, walking sticks, whips', gstRate: '18', classification: 'TAXABLE' },
  { code: '67', description: 'Prepared feathers, artificial flowers, human hair', gstRate: '18', classification: 'TAXABLE' },
  { code: '68', description: 'Articles of stone, plaster, cement, asbestos, mica', gstRate: '18', classification: 'TAXABLE' },
  { code: '69', description: 'Ceramic products', gstRate: '18', classification: 'TAXABLE' },
  { code: '70', description: 'Glass and glassware', gstRate: '18', classification: 'TAXABLE' },
  { code: '71', description: 'Natural or cultured pearls, precious stones and metals', gstRate: '3', classification: 'TAXABLE' },
  { code: '72', description: 'Iron and steel', gstRate: '18', classification: 'TAXABLE' },
  { code: '73', description: 'Articles of iron or steel', gstRate: '18', classification: 'TAXABLE' },
  { code: '74', description: 'Copper and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '75', description: 'Nickel and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '76', description: 'Aluminium and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '78', description: 'Lead and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '79', description: 'Zinc and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '80', description: 'Tin and articles thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '81', description: 'Other base metals, cermets', gstRate: '18', classification: 'TAXABLE' },
  { code: '82', description: 'Tools, implements, cutlery of base metal', gstRate: '18', classification: 'TAXABLE' },
  { code: '83', description: 'Miscellaneous articles of base metal', gstRate: '18', classification: 'TAXABLE' },
  { code: '84', description: 'Machinery and mechanical appliances, boilers', gstRate: '18', classification: 'TAXABLE' },
  { code: '85', description: 'Electrical machinery, equipment, sound recorders', gstRate: '18', classification: 'TAXABLE' },
  { code: '86', description: 'Railway locomotives and rolling stock', gstRate: '18', classification: 'TAXABLE' },
  { code: '87', description: 'Vehicles other than railway, their parts', gstRate: '28', classification: 'TAXABLE' },
  { code: '88', description: 'Aircraft, spacecraft and parts', gstRate: '5', classification: 'TAXABLE' },
  { code: '89', description: 'Ships, boats and floating structures', gstRate: '5', classification: 'TAXABLE' },
  { code: '90', description: 'Optical, photographic, measuring, medical instruments', gstRate: '18', classification: 'TAXABLE' },
  { code: '91', description: 'Clocks and watches and parts', gstRate: '18', classification: 'TAXABLE' },
  { code: '92', description: 'Musical instruments and parts', gstRate: '18', classification: 'TAXABLE' },
  { code: '93', description: 'Arms and ammunition', gstRate: '28', classification: 'TAXABLE' },
  { code: '94', description: 'Furniture, bedding, lamps, illuminated signs', gstRate: '18', classification: 'TAXABLE' },
  { code: '95', description: 'Toys, games, sports equipment', gstRate: '12', classification: 'TAXABLE' },
  { code: '96', description: 'Miscellaneous manufactured articles', gstRate: '18', classification: 'TAXABLE' },
  { code: '97', description: 'Works of art, collectors pieces, antiques', gstRate: '12', classification: 'TAXABLE' },
  { code: '98', description: 'Project imports and specific goods', gstRate: '18', classification: 'TAXABLE' },
  { code: '99', description: 'Services (SAC)', gstRate: '18', classification: 'TAXABLE' },
];

/**
 * A handful of codes the trading verticals actually sell, so a search for a product name finds a
 * code rather than only a chapter. Kept deliberately short — this is a helper, not a tariff.
 */
export const HSN_COMMON_CODES: ReadonlyArray<HsnEntry> = [
  { code: '0401', description: 'Milk and cream, not concentrated', gstRate: '5', classification: 'TAXABLE' },
  { code: '0902', description: 'Tea, whether or not flavoured', gstRate: '5', classification: 'TAXABLE' },
  { code: '1006', description: 'Rice', gstRate: '5', classification: 'TAXABLE' },
  { code: '1101', description: 'Wheat or meslin flour', gstRate: '5', classification: 'TAXABLE' },
  { code: '1701', description: 'Cane or beet sugar', gstRate: '5', classification: 'TAXABLE' },
  { code: '1704', description: 'Sugar confectionery, chewing gum', gstRate: '18', classification: 'TAXABLE' },
  { code: '1905', description: 'Bread, pastry, cakes, biscuits', gstRate: '18', classification: 'TAXABLE' },
  { code: '2106', description: 'Food preparations not elsewhere specified', gstRate: '18', classification: 'TAXABLE' },
  { code: '2201', description: 'Waters, aerated waters', gstRate: '18', classification: 'TAXABLE' },
  { code: '2202', description: 'Sweetened or flavoured waters, soft drinks', gstRate: '28', classification: 'TAXABLE' },
  { code: '2523', description: 'Portland cement, aluminous cements', gstRate: '28', classification: 'TAXABLE' },
  { code: '2710', description: 'Petroleum oils, lubricants', gstRate: '18', classification: 'TAXABLE' },
  { code: '3004', description: 'Medicaments, retail packs', gstRate: '12', classification: 'TAXABLE' },
  { code: '3208', description: 'Paints and varnishes in a non-aqueous medium', gstRate: '18', classification: 'TAXABLE' },
  { code: '3304', description: 'Beauty or make-up preparations, skin care', gstRate: '18', classification: 'TAXABLE' },
  { code: '3401', description: 'Soap, organic surface-active products', gstRate: '18', classification: 'TAXABLE' },
  { code: '3402', description: 'Detergents, washing preparations', gstRate: '18', classification: 'TAXABLE' },
  { code: '3923', description: 'Plastic articles for conveyance or packing', gstRate: '18', classification: 'TAXABLE' },
  { code: '3926', description: 'Other articles of plastics', gstRate: '18', classification: 'TAXABLE' },
  { code: '4011', description: 'New pneumatic tyres of rubber', gstRate: '28', classification: 'TAXABLE' },
  { code: '4818', description: 'Toilet paper, tissues, household paper', gstRate: '18', classification: 'TAXABLE' },
  { code: '6109', description: 'T-shirts, singlets and other vests, knitted', gstRate: '5', classification: 'TAXABLE' },
  { code: '6302', description: 'Bed linen, table linen, toilet linen', gstRate: '5', classification: 'TAXABLE' },
  { code: '6403', description: 'Footwear with leather uppers', gstRate: '18', classification: 'TAXABLE' },
  { code: '7210', description: 'Flat-rolled iron or steel, clad or coated', gstRate: '18', classification: 'TAXABLE' },
  { code: '7306', description: 'Other tubes and pipes of iron or steel', gstRate: '18', classification: 'TAXABLE' },
  { code: '7318', description: 'Screws, bolts, nuts, coach screws', gstRate: '18', classification: 'TAXABLE' },
  { code: '8415', description: 'Air conditioning machines', gstRate: '28', classification: 'TAXABLE' },
  { code: '8418', description: 'Refrigerators, freezers', gstRate: '18', classification: 'TAXABLE' },
  { code: '8443', description: 'Printing machinery, printers, copiers', gstRate: '18', classification: 'TAXABLE' },
  { code: '8471', description: 'Computers, laptops and data processing units', gstRate: '18', classification: 'TAXABLE' },
  { code: '8504', description: 'Electrical transformers, converters, adaptors', gstRate: '18', classification: 'TAXABLE' },
  { code: '8506', description: 'Primary cells and primary batteries', gstRate: '18', classification: 'TAXABLE' },
  { code: '8517', description: 'Telephones, smartphones, network equipment', gstRate: '18', classification: 'TAXABLE' },
  { code: '8528', description: 'Monitors and projectors, television receivers', gstRate: '18', classification: 'TAXABLE' },
  { code: '8544', description: 'Insulated wire, cable and other conductors', gstRate: '18', classification: 'TAXABLE' },
  { code: '8708', description: 'Parts and accessories of motor vehicles', gstRate: '28', classification: 'TAXABLE' },
  { code: '9403', description: 'Other furniture and parts thereof', gstRate: '18', classification: 'TAXABLE' },
  { code: '9503', description: 'Toys, tricycles, scooters, dolls', gstRate: '12', classification: 'TAXABLE' },
  { code: '9983', description: 'Professional, technical and business services (SAC)', gstRate: '18', classification: 'TAXABLE' },
];

const ALL_HSN: ReadonlyArray<HsnEntry> = [...HSN_COMMON_CODES, ...HSN_CHAPTERS];

/** HSN is 2, 4, 6 or 8 digits. An empty value is allowed (the operator may not know it yet). */
export function isValidHsnCode(raw: unknown): boolean {
  const text = String(raw ?? '').trim();
  if (!text) return true;
  return /^(\d{2}|\d{4}|\d{6}|\d{8})$/.test(text);
}

/** The message an operator sees when the format is wrong. */
export const HSN_FORMAT_MESSAGE =
  'An HSN code is 2, 4, 6 or 8 digits. Leave it blank if you do not know it yet.';

/**
 * Search chapters and common codes by code prefix or by description.
 *
 * A blank query returns the common codes first (the products most trading setups sell), capped so
 * a picker opening on an empty field is not a wall of 100 chapters.
 */
export function searchHsn(query: unknown, limit = 25): HsnEntry[] {
  const q = String(query ?? '').trim().toLowerCase();
  const cap = Math.max(1, Math.min(100, Math.floor(limit) || 25));
  if (!q) return ALL_HSN.slice(0, cap);

  const digits = q.replace(/\D/g, '');
  const matches = ALL_HSN.filter((entry) => {
    if (digits && entry.code.startsWith(digits)) return true;
    return entry.description.toLowerCase().includes(q);
  });
  // Exact code matches first, then the shortest (chapter before 8-digit detail), then code order.
  return matches
    .sort((a, b) => {
      if (a.code === digits && b.code !== digits) return -1;
      if (b.code === digits && a.code !== digits) return 1;
      if (a.code.length !== b.code.length) return a.code.length - b.code.length;
      return a.code.localeCompare(b.code);
    })
    .slice(0, cap);
}

/** The rate a chapter implies, for prefilling a master when only an HSN is known. */
export function hsnEntry(code: unknown): HsnEntry | null {
  const text = String(code ?? '').trim();
  if (!text) return null;
  return (
    ALL_HSN.find((entry) => entry.code === text) ??
    ALL_HSN.find((entry) => entry.code === text.slice(0, 2)) ??
    null
  );
}

export const HSN_LOOKUP_NOTE =
  'Chapter rates are a starting point, not a legal classification. Set the exact rate on the product.';
