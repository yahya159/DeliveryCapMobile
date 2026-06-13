module.exports = srv => {

  srv.before(['CREATE', 'UPDATE'], 'Deliveries', req => {
    const data = req.data;

    const allowedStatuses = [
      'CREATED',
      'ASSIGNED',
      'IN_PROGRESS',
      'ARRIVED',
      'DELIVERED',
      'FAILED',
      'CANCELLED'
    ];

    if (data.status && !allowedStatuses.includes(data.status)) {
      req.error(400, `Invalid status: ${data.status}`);
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