import SwaggerParser from '@apidevtools/swagger-parser';
import { writeFileSync } from 'fs';
import { join } from 'path';
import YAML from 'yaml';

const root = join(__dirname, '..');
const input = join(root, 'openapi', 'momentra-v1.yaml');
const output = join(root, 'openapi', 'momentra-v1.bundled.yaml');

async function main(): Promise<void> {
  const bundled = await SwaggerParser.bundle(input);
  writeFileSync(output, YAML.stringify(bundled), 'utf8');
  console.log('Bundled OpenAPI ->', output);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
