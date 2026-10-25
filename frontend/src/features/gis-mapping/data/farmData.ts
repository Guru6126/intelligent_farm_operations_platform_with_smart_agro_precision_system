
import type { Farm, FarmPlot } from "../types";

export const farms: Farm[] = [
  {
    id: "farm-1",
    name: "Green Valley Farm",
    location: "Hyderabad, Telangana",
    latitude: 17.385,
    longitude: 78.4867,
  },
  {
    id: "farm-2",
    name: "Sunrise Farm",
    location: "Warangal, Telangana",
    latitude: 17.9689,
    longitude: 79.5941,
  },
];

export const initialFarmPlots: FarmPlot[] = [
  {
    id: "plot-1",
    farmId: "farm-1",
    name: "Rice Plot",
    crop: "Rice",
    areaHectares: 0,
    boundary: [
      [17.386, 78.485],
      [17.386, 78.488],
      [17.384, 78.488],
      [17.384, 78.485],
    ],
  },
  {
    id: "plot-2",
    farmId: "farm-1",
    name: "Wheat Plot",
    crop: "Wheat",
    areaHectares: 0,
    boundary: [
      [17.3835, 78.485],
      [17.3835, 78.488],
      [17.3818, 78.488],
      [17.3818, 78.485],
    ],
  },
  {
    id: "plot-3",
    farmId: "farm-2",
    name: "Cotton Plot",
    crop: "Cotton",
    areaHectares: 0,
    boundary: [
      [17.9695, 79.5935],
      [17.9695, 79.596],
      [17.9675, 79.596],
      [17.9675, 79.5935],
    ],
  },
];
