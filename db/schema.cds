using { cuid, managed } from '@sap/cds/common';

namespace delivery.db;

entity Deliveries : cuid, managed {
  orderNo          : String(50);
  customerName     : String(120);
  customerPhone    : String(40);
  deliveryAddress  : String(500);
  status           : String(30) default 'CREATED';
  driverEmail      : String(255);
  note             : String(500);
}