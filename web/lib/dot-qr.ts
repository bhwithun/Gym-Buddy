import QRCode from "qrcode";

/** Circular modules on a white field. High error correction keeps a dotted code scannable. */
export function renderDotQr(text: string): string {
  const qr = QRCode.create(text, { errorCorrectionLevel: "H" });
  const count = qr.modules.size;
  const cell = 10;
  const margin = 4;
  const dimension = (count + margin * 2) * cell;
  const radius = cell * 0.42;
  const dots: string[] = [];
  for (let row = 0; row < count; row++) {
    for (let column = 0; column < count; column++) {
      if (!qr.modules.get(row, column)) continue;
      const cx = (column + margin) * cell + cell / 2;
      const cy = (row + margin) * cell + cell / 2;
      dots.push(`<circle cx="${cx}" cy="${cy}" r="${radius}"/>`);
    }
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${dimension} ${dimension}" role="img" aria-label="Profile QR code"><rect width="100%" height="100%" fill="#ffffff"/>${dots.join("")}</svg>`;
}
