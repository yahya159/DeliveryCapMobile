using delivery.db as db from '../db/schema';

service DeliveryService @(path: '/delivery') {
  entity Deliveries as projection on db.Deliveries;
}