import React, { createContext, useContext, useState, useEffect } from 'react';
import { audioAlerts } from '../utils/audioAlert';

export type NotificationType = 'SUCCESS' | 'FRAUD_ALERT' | 'ESCROW_REFUND' | 'SECURITY';

export interface FraudDetail {
  threatCategory: string;
  similarityMatch: number;
  matchedPatternDescription: string;
  merchantName: string;
  merchantUpi: string;
  merchantTrustScore: number;
  reportedCount: number;
  action: 'BLOCK' | 'FLAG_VERIFICATION' | 'ALLOW';
  refundId?: string;
  refundAmount?: number;
}

export interface NotificationItem {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  amount?: number;
  upiId?: string;
  referenceId?: string;
  fraudDetails?: FraudDetail;
}

interface NotificationContextType {
  notifications: NotificationItem[];
  unreadCount: number;
  activeToast: NotificationItem | null;
  isSoundEnabled: boolean;
  toggleSound: () => void;
  addNotification: (n: Omit<NotificationItem, 'id' | 'timestamp' | 'isRead'>) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  deleteNotification: (id: string) => void;
  clearAll: () => void;
  closeToast: () => void;
  simulateSuccess: (amount?: number, upiId?: string) => void;
  simulateFraudAndRefund: (amount?: number, upiId?: string) => void;
}

const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 'notif-1',
    type: 'FRAUD_ALERT',
    title: '🚨 High-Risk Fraud Blocked & Escrow Refund Initiated',
    message: 'Payment of ₹5,000 to customercare_kyc@ybl was intercepted before settlement. Merchant trust score is 0%.',
    timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
    isRead: false,
    amount: 5000,
    upiId: 'customercare_kyc@ybl',
    referenceId: 'TXN_BLOCKED_98214',
    fraudDetails: {
      threatCategory: 'KYC_EXPIRATION_PHISHING',
      similarityMatch: 94.2,
      matchedPatternDescription: 'Your Paytm KYC has expired. Complete your KYC verification now by sharing your Aadhaar number...',
      merchantName: 'KYC Update Desk (Fake)',
      merchantUpi: 'customercare_kyc@ybl',
      merchantTrustScore: 0.0,
      reportedCount: 18,
      action: 'BLOCK',
      refundId: 'rzp_rfnd_92817482',
      refundAmount: 5000
    }
  },
  {
    id: 'notif-2',
    type: 'ESCROW_REFUND',
    title: '🛡️ SafePe Escrow Refund Completed',
    message: '₹5,000.00 has been credited back to your HDFC Bank (•••• 8842). Escrow protection activated.',
    timestamp: new Date(Date.now() - 4 * 60 * 1000).toISOString(),
    isRead: false,
    amount: 5000,
    upiId: 'customercare_kyc@ybl',
    referenceId: 'rzp_rfnd_92817482',
    fraudDetails: {
      threatCategory: 'KYC_EXPIRATION_PHISHING',
      similarityMatch: 94.2,
      matchedPatternDescription: 'Automated clawback from nodal account before merchant settlement.',
      merchantName: 'KYC Update Desk (Fake)',
      merchantUpi: 'customercare_kyc@ybl',
      merchantTrustScore: 0.0,
      reportedCount: 18,
      action: 'BLOCK',
      refundId: 'rzp_rfnd_92817482',
      refundAmount: 5000
    }
  },
  {
    id: 'notif-3',
    type: 'SUCCESS',
    title: '✅ Payment Successful',
    message: 'Payment of ₹1,450 to BESCOM Electricity Bill was completed securely via Bharat BillPay (BBPS).',
    timestamp: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
    isRead: true,
    amount: 1450,
    upiId: 'bescom@karnataka.gov.in',
    referenceId: 'BBPS_BESCOM_91823'
  },
  {
    id: 'notif-4',
    type: 'SECURITY',
    title: '🔐 Token Vault Security Update',
    message: 'Your HDFC Bank account was tokenized under PCI-DSS compliance standards with AES-256 vault encryption.',
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    isRead: true,
    referenceId: 'VAULT_TOKEN_8842'
  }
];

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    const saved = localStorage.getItem('safepe_notifications');
    return saved ? JSON.parse(saved) : INITIAL_NOTIFICATIONS;
  });

  const [activeToast, setActiveToast] = useState<NotificationItem | null>(null);
  const [isSoundEnabled, setIsSoundEnabled] = useState<boolean>(() => audioAlerts.isEnabled());

  useEffect(() => {
    localStorage.setItem('safepe_notifications', JSON.stringify(notifications));
  }, [notifications]);

  const toggleSound = () => {
    const newState = audioAlerts.toggleSound();
    setIsSoundEnabled(newState);
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  const addNotification = (n: Omit<NotificationItem, 'id' | 'timestamp' | 'isRead'>) => {
    const newNotif: NotificationItem = {
      ...n,
      id: `notif-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`,
      timestamp: new Date().toISOString(),
      isRead: false
    };

    // Play synthesized acoustic chime for this notification type!
    audioAlerts.playSoundForType(n.type);

    setNotifications(prev => [newNotif, ...prev]);
    setActiveToast(newNotif);

    // Auto-dismiss toast after 6 seconds
    setTimeout(() => {
      setActiveToast(current => (current?.id === newNotif.id ? null : current));
    }, 6000);
  };

  const markAsRead = (id: string) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
  };

  const markAllAsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
  };

  const deleteNotification = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  };

  const clearAll = () => {
    setNotifications([]);
  };

  const closeToast = () => {
    setActiveToast(null);
  };

  // Helper simulations for immediate demo & testing
  const simulateSuccess = (amount = 500, upiId = 'reliance.retail@oksbi') => {
    addNotification({
      type: 'SUCCESS',
      title: '✅ Transaction Successful',
      message: `Payment of ₹${amount.toLocaleString('en-IN')} to ${upiId} was completed successfully via Razorpay UPI.`,
      amount,
      upiId,
      referenceId: `TXN_${Date.now().toString().slice(-6)}`
    });
  };

  const simulateFraudAndRefund = (amount = 2500, upiId = 'urgent.refund892@ybl') => {
    // 1. Immediately fire the Fraud Alert Notification
    addNotification({
      type: 'FRAUD_ALERT',
      title: '🚨 High-Risk Fraud Intercepted by SafePe AI',
      message: `Payment of ₹${amount.toLocaleString('en-IN')} to ${upiId} was blocked. Threat: Impersonation & Fake Refund Scam.`,
      amount,
      upiId,
      referenceId: `TXN_BLOCKED_${Date.now().toString().slice(-5)}`,
      fraudDetails: {
        threatCategory: 'IMPERSONATION_REFUND_SCAM',
        similarityMatch: 96.5,
        matchedPatternDescription: 'I am calling from customer care. Your order has a refund of Rs 5000. Enter UPI PIN to claim...',
        merchantName: 'Fake Customer Refund Desk',
        merchantUpi: upiId,
        merchantTrustScore: 0.05,
        reportedCount: 24,
        action: 'BLOCK',
        refundId: `rzp_rfnd_${Date.now().toString().slice(-8)}`,
        refundAmount: amount
      }
    });

    // 2. Fire the Escrow Refund Completed notification 2 seconds later (matching background Kafka speed)
    setTimeout(() => {
      addNotification({
        type: 'ESCROW_REFUND',
        title: '🛡️ SafePe Escrow Refund Completed',
        message: `₹${amount.toLocaleString('en-IN')}.00 has been refunded to your bank. Escrow auto-clawback protected your funds.`,
        amount,
        upiId,
        referenceId: `rzp_rfnd_${Date.now().toString().slice(-8)}`,
        fraudDetails: {
          threatCategory: 'IMPERSONATION_REFUND_SCAM',
          similarityMatch: 96.5,
          matchedPatternDescription: 'Escrow auto-clawback executed before settlement cycle.',
          merchantName: 'Fake Customer Refund Desk',
          merchantUpi: upiId,
          merchantTrustScore: 0.05,
          reportedCount: 24,
          action: 'BLOCK',
          refundId: `rzp_rfnd_${Date.now().toString().slice(-8)}`,
          refundAmount: amount
        }
      });
    }, 2200);
  };

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        activeToast,
        isSoundEnabled,
        toggleSound,
        addNotification,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        clearAll,
        closeToast,
        simulateSuccess,
        simulateFraudAndRefund
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};
