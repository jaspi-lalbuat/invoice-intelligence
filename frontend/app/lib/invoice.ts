export type ProcessingStatus =
  | "QUEUED"
  | "PROCESSING"
  | "READY"
  | "REVIEW_REQUIRED"
  | "FAILED";

export type InvoiceResponse = {
  jobId: string;
  status: ProcessingStatus;

  // Active jobs legitimately have no invoice yet.
  invoice: {
    invoiceNumber: string;
    invoiceDate: string;
    dueDate: string;
    currency: string;

    supplier: {
      name: string;
      address: string | null;
      taxId: string | null;
    };

    customer: {
      name: string;
      address: string | null;
      taxId: string | null;
    };

    lineItems: {
      description: string;
      quantity: number;
      unitPrice: number;
      taxRate: number | null;
      statedTotal: number | null;
    }[];

    subtotal: number;

    taxes: {
      cgst: number | null;
      sgst: number | null;
      igst: number | null;
    };

    discount: number | null;
    total: number;
  } | null;

  validation: {
    status: string;
    calculatedSubtotal: number;
    subtotalDifference: number;
    calculatedTotal: number;
    totalDifference: number;

    issues: {
      code: string;
      message: string;
      difference: number;
    }[];
  } | null;

  createdAt: string;
  updatedAt: string;
};

export type InvoiceSummary = {
  jobId: string;
  originalFileName: string;
  status: ProcessingStatus;
  createdAt: string;
  updatedAt: string;
};

export const formatStatus = (status: ProcessingStatus) =>
  status.replaceAll("_", " ");

export const getStatusColor = (status: ProcessingStatus) => {
  switch (status) {
    case "READY":
      return "bg-emerald-400";

    case "REVIEW_REQUIRED":
      return "bg-amber-400";

    case "FAILED":
      return "bg-red-400";

    case "PROCESSING":
      return "bg-blue-400";

    case "QUEUED":
    default:
      return "bg-zinc-500";
  }
};

export const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();

  const seconds = Math.floor(
    (now.getTime() - date.getTime()) / 1000,
  );

  if (seconds < 60) {
    return "just now";
  }

  const minutes = Math.floor(seconds / 60);

  if (minutes < 60) {
    return `${minutes}m ago`;
  }

  const hours = Math.floor(minutes / 60);

  if (hours < 24) {
    return `${hours}h ago`;
  }

  const days = Math.floor(hours / 24);

  return `${days}d ago`;
};