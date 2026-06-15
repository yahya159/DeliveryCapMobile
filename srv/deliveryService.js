module.exports = srv => {

  srv.before(['CREATE', 'UPDATE'], 'Deliveries', req => {
    const data = req.data;
    const requiredFields = ['orderNo', 'customerName', 'deliveryAddress'];

    const allowedStatuses = [
      'CREATED',
      'ASSIGNED',
      'IN_PROGRESS',
      'ARRIVED',
      'DELIVERED',
      'FAILED',
      'CANCELLED'
    ];

    for (const field of requiredFields) {
      if (Object.prototype.hasOwnProperty.call(data, field) && typeof data[field] === 'string') {
        data[field] = data[field].trim();
      }
      if (req.event === 'CREATE' && (data[field] === undefined || data[field] === null || data[field] === '')) {
        req.error(400, `${field} is required`);
      }
      if (req.event === 'UPDATE' && (data[field] === null || data[field] === '')) {
        req.error(400, `${field} cannot be empty`);
      }
    }

    if (typeof data.status === 'string') {
      data.status = data.status.trim().toUpperCase();
    }

    if (data.status && !allowedStatuses.includes(data.status)) {
      req.error(400, `Invalid status: ${data.status}`);
    }

    if (data.driverEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.driverEmail)) {
      req.error(400, 'driverEmail must be a valid email address');
    }

    if (data.latitude !== undefined && data.latitude !== null) {
      if (data.latitude < -90 || data.latitude > 90) {
        req.error(400, 'Latitude must be between -90 and 90');
      }
    }

    if (data.longitude !== undefined && data.longitude !== null) {
      if (data.longitude < -180 || data.longitude > 180) {
        req.error(400, 'Longitude must be between -180 and 180');
      }
    }
  });

};
