# Ace Restaurant — Android Mobile Ordering App

**Ace Restaurant** is a native Android mobile application that simulates a full-featured restaurant ordering experience. The app allows users to browse categorized menus, customize items, manage a cart, and complete a secure checkout flow — all designed with scalability, usability, and real-world business requirements in mind.

This project was developed as a capstone-style application for **IT 633: Mobile Application Development** at **Southern New Hampshire University**, with an emphasis on clean architecture, user-centered design, and ethical software practices.

---

## Features

### Menu Browsing & Search
- Categorized menus (Appetizers, Entrées, Desserts)
- Real-time, case-insensitive search across all categories
- Item cards with images, descriptions, and pricing

### Item Customization
- Quantity adjustments and configurable options
- Dynamic price recalculation based on selections

### Cart Management
- Add, edit, or remove items
- Automatic subtotal, tax, and total calculations
- Shared cart state across fragments

### Checkout & Validation
- Delivery and payment forms with inline validation
- Credit card auto-formatting and length verification
- “Place Order” button dynamically enabled/disabled

### Order Confirmation
- Unique order confirmation number
- Estimated delivery time simulation

### Accessibility & UX
- Material Design–based UI
- Content descriptions for screen readers
- High-contrast color palette and readable typography
- Fixed navigation for consistent user control

---

## Architecture & Design

### Architecture Pattern
- MVVM-inspired separation of concerns
- Shared `ViewModel` for cart and checkout state
- Modular, maintainable fragment-based navigation

### Android Stack
- Android Studio
- Kotlin & XML
- Jetpack Components:
  - ViewModel
  - LiveData
  - Navigation Component
- RecyclerView, CardView, ConstraintLayout

### Design Principles
- User-Centered Design (UCD)
- Material Design guidelines
- Responsive layouts for multiple screen sizes
- Clear visual hierarchy for faster decision-making

---

## Security & Ethics

- Input sanitization and defensive validation
- Payment-ready architecture aligned with **PCI DSS v4.0**
- GDPR-conscious data handling (no unnecessary data retention)
- Accessibility aligned with **WCAG 2.1 AA**
- No third-party tracking or invasive analytics

---

## Scalability & Future Enhancements

The application is structured to support future growth, including:

- Integration with real payment gateways (Stripe / PayPal)
- Firebase or Room database persistence
- Order history and user profiles
- Loyalty programs and push notifications
- Real-time order tracking

---

## Development Approach

- Iterative, Agile-style development
- Feature-by-feature implementation with usability testing
- Edge-case handling for user input and navigation
- Pseudocode-driven logic design to ensure clarity before implementation

---

## Tech Stack

- **Language:** Kotlin  
- **Platform:** Android (Native)  
- **UI:** XML + Material Design  
- **Architecture:** MVVM-style  
- **Tools:** Android Studio, GitHub  

---

## Author

**Tevin Davis**  
Graduate Student — M.S. Information Technology (Application Development)  
Southern New Hampshire University  

*This project demonstrates my ability to design, architect, and implement a production-ready mobile application with strong UX, clean structure, and ethical considerations.*
