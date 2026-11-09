import sharp from 'sharp';
const file = process.argv[2];
const points = JSON.parse(process.argv[3]);
const { data, info } = await sharp(file).raw().toBuffer({ resolveWithObject: true });
const ch = info.channels;
for (const [name, x, y] of points) {
  const i = (y * info.width + x) * ch;
  const hex = [data[i], data[i+1], data[i+2]].map(v => v.toString(16).padStart(2,'0')).join('').toUpperCase();
  console.log(`${name.padEnd(22)} (${x},${y})  #${hex}`);
}
