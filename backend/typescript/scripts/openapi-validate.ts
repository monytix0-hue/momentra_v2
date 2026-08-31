import SwaggerParser from '@apidevtools/swagger-parser';
import { join } from 'path';

const specs = [
  join(__dirname, '..', 'openapi', 'momentra-v1.yaml'),
  join(__dirname, '..', 'openapi', 'health.yaml'),
];

async function main(): Promise<void> {
  for (const spec of specs) {
    const api = await SwaggerParser.validate(spec);
    const pathCount = Object.keys(api.paths ?? {}).length;
    console.log(`PASS ${spec} (${pathCount} paths)`);
  }
}

main().catch((err) => {
  console.error('OpenAPI validation FAIL:', err.message ?? err);
  process.exit(1);
});
