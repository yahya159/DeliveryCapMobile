using { cuid, managed } from '@sap/cds/common';

namespace delivery.db;

entity Deliveries : cuid, managed {
  orderNo          : String(50) not null;
  customerName     : String(120) not null;
  customerPhone    : String(40);
  deliveryAddress  : String(500) not null;
  status           : String(30) not null default 'CREATED';
  driverEmail      : String(255);
  note             : String(500);
  latitude         : Double;
  longitude        : Double;
}
