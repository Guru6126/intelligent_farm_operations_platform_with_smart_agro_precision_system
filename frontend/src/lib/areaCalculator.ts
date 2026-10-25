
export type GeoPoint = [number, number]; // [latitude, longitude]

const EARTH_RADIUS_METERS = 6_378_137;

/**
 * Calculates approximate polygon area in square meters.
 * Supports any polygon with at least three points.
 */
export function calculateAreaSquareMeters(
  points: GeoPoint[],
): number {
  if (points.length < 3) return 0;

  let sum = 0;

  for (let i = 0; i < points.length; i += 1) {
    const [lat1, lon1] = points[i];
    const [lat2, lon2] = points[(i + 1) % points.length];

    const lat1Rad = (lat1 * Math.PI) / 180;
    const lat2Rad = (lat2 * Math.PI) / 180;
    const lon1Rad = (lon1 * Math.PI) / 180;
    const lon2Rad = (lon2 * Math.PI) / 180;

    sum +=
      (lon2Rad - lon1Rad) *
      (2 + Math.sin(lat1Rad) + Math.sin(lat2Rad));
  }

  return Math.abs(
    (sum * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS) / 2,
  );
}

export function calculateAreaHectares(
  points: GeoPoint[],
): number {
  return calculateAreaSquareMeters(points) / 10_000;
}

export function calculateAreaAcres(
  points: GeoPoint[],
): number {
  return calculateAreaSquareMeters(points) / 4046.8564224;
}
