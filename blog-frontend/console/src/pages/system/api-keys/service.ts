import { http } from '@/utils';

export interface ApiKeyItem {
  id: number;
  name: string;
  createdAt: string;
  lastUsedAt: string | null;
  revokedAt: string | null;
}

export interface CreatedApiKey {
  apiKey: ApiKeyItem;
  value: string;
}

export async function listApiKeys(): Promise<ApiKeyItem[]> {
  const response = await http.get('/api/console/api-keys');
  return response.data;
}

export async function createApiKey(name: string): Promise<CreatedApiKey> {
  const response = await http.post('/api/console/api-keys', { name });
  return response.data;
}

export async function revokeApiKey(id: number): Promise<void> {
  await http.delete(`/api/console/api-keys/${id}`);
}
