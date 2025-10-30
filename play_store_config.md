# Google Play Store Product Configuration

## In-App Product Setup

### Product ID: `storage_sentinel_pro`
**Type:** One-time In-App Product  
**Title:** Storage Sentinel PRO  
**Description:** Unlock advanced storage cleaning features including residual data removal, large file scanning, and intelligent duplicate file detection.

### Pricing Tiers by Country:
- **United States:** $4.99 USD
- **Canada:** $6.99 CAD  
- **United Kingdom:** £4.49 GBP
- **European Union:** €4.99 EUR
- **Australia:** $7.99 AUD
- **India:** ₹399 INR
- **Japan:** ¥600 JPY

### Features Unlocked:
1. **Residual App Data Cleaning** - Remove leftover files from uninstalled apps
2. **Large File Detection** - Find files over 100MB for space optimization
3. **Smart Duplicate Removal** - Advanced content-based duplicate detection
4. **Unlimited Cleaning Sessions** - No daily limits on storage optimization
5. **Priority Support** - Faster response times for customer support

### Play Console Configuration Steps:
1. Go to Google Play Console → Your App → Monetize → Products → In-app products
2. Create new product with ID: `storage_sentinel_pro`
3. Set title and description as above
4. Configure pricing for all target countries
5. Set status to "Active" when ready for production
6. Test with license testing accounts before release

### Implementation Notes:
- Product purchase is verified server-side through Google Play
- Purchase state is persisted locally using DataStore
- Graceful fallback to simulation mode for testing
- All purchases are acknowledged to prevent refund abuse
- Support for purchase restoration on app reinstall

### Testing:
- Use Google Play Console license testing
- Test accounts can make purchases without being charged
- Verify purchase flow on multiple devices and Android versions
- Test purchase restoration after app reinstall