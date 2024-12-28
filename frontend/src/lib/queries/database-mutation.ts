import { request } from '@/lib/queries/utils.ts';

const UpdateDatabaseMutationDocument = /* GraphQL */ `
  mutation {
    Update
  }
`;

export async function UpdateDatabase() {
  await request(UpdateDatabaseMutationDocument);
}

const BuildDatabaseMutationDocument = /* GraphQL */ `
  mutation {
    Build
  }
`;

export async function BuildDatabase() {
  await request(BuildDatabaseMutationDocument);
}
