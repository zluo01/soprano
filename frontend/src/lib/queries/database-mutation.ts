import { request } from '@/lib/queries/utils.ts';

const UpdateDatabaseMutationDocument = /* GraphQL */ `
  mutation {
    Update
  }
`;

export async function updateDatabase() {
  await request(UpdateDatabaseMutationDocument);
}

const BuildDatabaseMutationDocument = /* GraphQL */ `
  mutation {
    Build
  }
`;

export async function buildDatabase() {
  await request(BuildDatabaseMutationDocument);
}
