import {
  InvoiceResponse,
  InvoiceSummary,
} from "./invoice";

export const API_BASE_URL = "http://localhost:8080";

export async function getInvoices(): Promise<InvoiceSummary[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/documents`,
  );

  if (!response.ok) {
    throw new Error(
      `Failed to load invoices (${response.status})`,
    );
  }

  return response.json();
}

export async function getInvoice(
  jobId: string,
): Promise<InvoiceResponse> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/documents/${jobId}`,
    {
      cache: "no-store",
    },
  );

  if (!response.ok) {
    throw new Error(
      `Failed to fetch invoice (${response.status})`,
    );
  }

  return response.json();
}

export async function uploadInvoice(
  file: File,
): Promise<{ jobId: string }> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(
    `${API_BASE_URL}/api/v1/documents`,
    {
      method: "POST",
      body: formData,
    },
  );

  if (!response.ok) {
    throw new Error(
      `Upload failed (${response.status})`,
    );
  }

  return response.json();
}

export async function retryInvoice(
  jobId: string,
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/documents/${jobId}/retry`,
    {
      method: "POST",
    },
  );

  if (!response.ok) {
    throw new Error(
      `Retry failed (${response.status})`,
    );
  }
}

export function downloadInvoiceUrl(jobId: string) {
  return `${API_BASE_URL}/api/v1/documents/${jobId}/file`;
}