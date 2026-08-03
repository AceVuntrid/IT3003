import { Box, Button, Dialog, DialogActions, DialogContent, Divider, GlobalStyles, Stack, Typography } from '@mui/material';
import PrintOutlinedIcon from '@mui/icons-material/PrintOutlined';
import type { Payment } from '../../api/types';
import { displayFont, monoFont } from '../../theme';
import { formatDateTime, formatMoney, titleCase } from '../../utils/format';

const STAMPS: Record<string, { label: string; color: string }> = {
  PAID: { label: 'PAID', color: '#1B7A3D' },
  PENDING: { label: 'PENDING', color: '#B45309' },
  PARTIALLY_REFUNDED: { label: 'PART. REFUNDED', color: '#6B7280' },
  REFUNDED: { label: 'REFUNDED', color: '#6B7280' },
  CANCELLED: { label: 'CANCELLED', color: '#6B7280' },
};

const printStyles = (
  <GlobalStyles
    styles={{
      '@media print': {
        'body *': { visibility: 'hidden' },
        '.invoice-print-area, .invoice-print-area *': { visibility: 'visible' },
        '.invoice-print-area': { position: 'absolute', left: 0, top: 0, margin: 0, boxShadow: 'none' },
      },
    }}
  />
);

/** Screenshot-friendly invoice / receipt view of a payment. */
export default function InvoiceDialog({ payment, onClose }: { payment: Payment | null; onClose: () => void }) {
  const stamp = payment ? STAMPS[payment.status] ?? { label: titleCase(payment.status).toUpperCase(), color: '#6B7280' } : null;
  const isPaid = !!payment && payment.status !== 'PENDING' && payment.status !== 'CANCELLED';
  const description = payment
    ? payment.description || payment.assetName || titleCase(payment.transactionType)
    : '';

  return (
    <Dialog open={!!payment} onClose={onClose} maxWidth={false}>
      {payment && printStyles}
      {payment && stamp && (
        <DialogContent sx={{ p: 0 }}>
          <Box className="invoice-print-area"
               sx={{ width: 480, maxWidth: '100%', backgroundColor: '#fff', color: '#1F2430', p: 4 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
              <Box>
                <Typography sx={{ fontFamily: displayFont, fontWeight: 700, fontSize: '1.05rem', lineHeight: 1.25 }}>
                  University Asset Management
                </Typography>
                <Typography sx={{ fontFamily: monoFont, fontSize: '0.62rem', color: 'text.secondary', letterSpacing: '0.14em' }}>
                  FACULTY OF SCIENCE
                </Typography>
              </Box>
              <Box aria-label={`Status: ${stamp.label}`}
                   sx={{
                     border: '3px solid', borderColor: stamp.color, color: stamp.color, borderRadius: 1,
                     px: 1.25, py: 0.25, transform: 'rotate(-8deg)', opacity: 0.9, mt: 0.5,
                     fontFamily: monoFont, fontWeight: 700, fontSize: '0.82rem',
                     letterSpacing: '0.12em', whiteSpace: 'nowrap',
                   }}>
                {stamp.label}
              </Box>
            </Stack>

            <Divider sx={{ my: 2.5 }} />

            <Stack direction="row" justifyContent="space-between" alignItems="flex-end" sx={{ mb: 2.5 }}>
              <Box>
                <Typography variant="overline" sx={{ letterSpacing: '0.16em', color: 'text.secondary' }}>
                  Invoice
                </Typography>
                <Typography sx={{ fontFamily: monoFont, fontWeight: 600, fontSize: '1.05rem' }}>
                  {payment.transactionNumber}
                </Typography>
              </Box>
              <Box sx={{ textAlign: 'right' }}>
                <Typography variant="caption" color="text.secondary" display="block">
                  {formatDateTime(payment.paymentDate)}
                </Typography>
                <Typography variant="body2">
                  Billed to: <b>{payment.payerDisplayName ?? '—'}</b>
                </Typography>
              </Box>
            </Stack>

            <Stack direction="row" justifyContent="space-between" spacing={2}
                   sx={{ borderBottom: '2px solid #1F2430', pb: 0.5 }}>
              <Typography variant="overline" sx={{ letterSpacing: '0.12em', color: 'text.secondary' }}>
                Description
              </Typography>
              <Typography variant="overline" sx={{ letterSpacing: '0.12em', color: 'text.secondary' }}>
                Amount
              </Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}
                   sx={{ py: 1.25, borderBottom: '1px solid', borderColor: 'divider' }}>
              <Box>
                <Typography variant="body2">{description}</Typography>
                {payment.reservationNumber && (
                  <Typography variant="caption" color="text.secondary" sx={{ fontFamily: monoFont }}>
                    Reservation {payment.reservationNumber}
                  </Typography>
                )}
              </Box>
              <Typography variant="body2" sx={{ whiteSpace: 'nowrap' }}>
                {formatMoney(payment.amount, payment.currency)}
              </Typography>
            </Stack>
            {payment.refundedAmount > 0 && (
              <Stack direction="row" justifyContent="space-between" spacing={2}
                     sx={{ py: 1, borderBottom: '1px solid', borderColor: 'divider' }}>
                <Typography variant="body2" color="text.secondary">Refunded</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                  −{formatMoney(payment.refundedAmount, payment.currency)}
                </Typography>
              </Stack>
            )}
            <Stack direction="row" justifyContent="space-between" spacing={2} sx={{ py: 1.25 }}>
              <Typography variant="body2" sx={{ fontWeight: 700 }}>Total</Typography>
              <Typography variant="body2" sx={{ fontWeight: 700, whiteSpace: 'nowrap' }}>
                {formatMoney(payment.amount - payment.refundedAmount, payment.currency)}
              </Typography>
            </Stack>

            {isPaid && payment.paymentMethod && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Paid via {payment.paymentMethod}
                {payment.referenceNumber ? ` · Ref ${payment.referenceNumber}` : ''}
              </Typography>
            )}

            <Typography variant="caption" color="text.secondary"
                        sx={{ display: 'block', textAlign: 'center', mt: 3, fontStyle: 'italic' }}>
              Keep this as your receipt.
            </Typography>
          </Box>
        </DialogContent>
      )}
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" startIcon={<PrintOutlinedIcon />} onClick={() => window.print()}>
          Print
        </Button>
        <Button variant="contained" onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
