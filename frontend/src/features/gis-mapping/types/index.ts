
export const CROP_TYPES = ["Rice", "Wheat", "Cotton", "Maize"] as const;

export type CropType = (typeof CROP_TYPES)[number];

export type GeoPoint = [number, number];

export interface Farm {
  id: string;
  name: string;
  location: string;
  latitude: number;
  longitude: number;
}

export interface FarmPlot {
  id: string;
  farmId: string;
  name: string;
  crop: CropType;
  areaHectares: number;
  boundary: GeoPoint[];
}
