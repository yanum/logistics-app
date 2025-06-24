This format:

{ "booking": "BK123", "containers": [ { "container": "MEDU1234567" } ], "orders": [ { "purchase": "PO123", "invoices": [ { "invoice": "IN123" } ] } ] }

Does not explicitly link containers to purchase orders.
Does not ensure that the same container isn't duplicated across multiple bookings.
Does not prevent different purchase orders from referencing the same container without control.
Since there’s no direct relationship between containers and orders and both are linked to the same booking, it's assumed that all containers and orders under a given booking are related to each other.

The current data model doesn’t support a one-to-one mapping between containers and orders. If that’s needed, we'd have to introduce an explicit link—like adding an orderId reference in the container object (or the other way around).
