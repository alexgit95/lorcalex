## 1. Detail View Control

- [x] 1.1 Add a wanted toggle control to the existing card detail modal for both owned and unowned cards.
- [x] 1.2 Render state-specific accessible text that distinguishes adding from removing the card from the wanted list.
- [x] 1.3 Keep the existing quantity controls and add-card actions unchanged when the wanted control is used.

## 2. State Synchronization

- [x] 2.1 Call the existing `api.setWanted` endpoint from the detail control and update the modal with the returned card DTO.
- [x] 2.2 Synchronize the updated wanted state in collection, recent-scan, pricing, and owned-pricing card caches.
- [x] 2.3 Preserve the gold-border display rule so it applies only to wanted cards that are not owned.
- [x] 2.4 Handle a failed toggle without changing the displayed state and surface the existing error feedback.

## 3. Verification

- [x] 3.1 Verify adding and removing wanted from an unowned card detail leaves ownership quantities unchanged.
- [x] 3.2 Verify adding and removing wanted from an owned card detail leaves regular and foil quantities unchanged.
- [x] 3.3 Verify the updated state is visible after closing and reopening the detail and after returning to the originating list.
- [x] 3.4 Run the project test suite and perform the relevant frontend interaction checks.

## 4. Documentation

- [x] 4.1 Update the relevant README collection or card-detail documentation to explain that wanted can be added or removed for owned and unowned cards.
- [x] 4.2 Add an `[Unreleased]` CHANGELOG entry describing the wanted toggle in card detail and its preservation of ownership quantities.
