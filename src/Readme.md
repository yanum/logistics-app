This format:

`{
    "booking": "BK123",
    "containers": [
        { "container": "MEDU1234567" }
    ],
    "orders": [
        {
            "purchase": "PO123",
            "invoices": [ { "invoice": "IN123" } ]
        }
    ]
}`

- Does not explicitly link containers to purchase orders.
- Does not ensure that the same container isn't duplicated across multiple bookings.
- Does not prevent different purchase orders from referencing the same container without control.
